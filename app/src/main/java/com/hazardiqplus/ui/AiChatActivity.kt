package com.hazardiqplus.ui

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import okhttp3.ResponseBody
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.hazardiqplus.R
import com.hazardiqplus.adapters.ChatMessageAdapter
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.*
import com.hazardiqplus.services.ChatHistoryResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AiChatActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var input: EditText
    private lateinit var sendBtn: ImageButton
    private lateinit var adapter: ChatMessageAdapter
    private val messages = ArrayList<ChatMessage>()
    private var currentUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_chat)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.title = "AI Chat"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        recycler = findViewById(R.id.chatRecyclerView)
        input = findViewById(R.id.messageInput)
        sendBtn = findViewById(R.id.sendButton)

        adapter = ChatMessageAdapter(messages)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        currentUid = Firebase.auth.currentUser?.uid

        loadHistory()

        sendBtn.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            sendMessageToServer(text)
            input.text.clear()
        }
    }

    private fun loadHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            Firebase.auth.currentUser?.getIdToken(true)?.addOnSuccessListener { idTokenResult ->
                val idToken = idTokenResult.token ?: return@addOnSuccessListener
                RetrofitClient.instance.getHistory(idToken).enqueue(object : Callback<ChatHistoryResponse> {
                    override fun onResponse(call: Call<ChatHistoryResponse>, response: Response<ChatHistoryResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            messages.clear()
                            for (item in response.body()!!.history) {
                                val isMine = item.role == "user"
                                messages.add(ChatMessage(item.message, "","",item.ts * 1000L,isMine ))
                            }
                            adapter.notifyDataSetChanged()
                            recycler.scrollToPosition(messages.size - 1)
                        }
                    }

                    override fun onFailure(call: Call<ChatHistoryResponse>, t: Throwable) {
                        Log.e("AI_CHAT", "history failed", t)
                    }
                })
            }?.addOnFailureListener {
                Log.e("AI_CHAT", "can't get id token: ${it.message}")
            }
        }
    }

    private fun sendMessageToServer(msg: String) {

        val now = System.currentTimeMillis()
        messages.add(ChatMessage(msg, "","",now,true, ))
        adapter.notifyItemInserted(messages.size - 1)
        recycler.scrollToPosition(messages.size - 1)

        Firebase.auth.currentUser?.getIdToken(true)?.addOnSuccessListener { idTokenResult ->
            val idToken = idTokenResult.token ?: return@addOnSuccessListener
            val req = AiChatRequest(msg)
            RetrofitClient.instance.sendMessage(idToken, req).enqueue(object : Callback<AiChatResponse> {
                override fun onResponse(call: Call<AiChatResponse>, response: Response<AiChatResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()

                        val answer = body?.model?.response ?: "No reply from server"
                        messages.add(ChatMessage(answer, "", "", System.currentTimeMillis(), false))
                        adapter.notifyItemInserted(messages.size - 1)
                        recycler.scrollToPosition(messages.size - 1)
                    } else {
                        Log.e("AI_CHAT", "send failed: ${response.code()}")
                        Toast.makeText(this@AiChatActivity, "Send failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AiChatResponse>, t: Throwable) {
                    Log.e("AI_CHAT", "send error", t)
                    Toast.makeText(this@AiChatActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
        }?.addOnFailureListener {
            Log.e("AI_CHAT", "token fetch failed: ${it.message}")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_ai_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            android.R.id.home -> {
                finish()
                true
            }

            R.id.action_restart -> {
                restartConversation()
                return true
            }
            R.id.action_clear_local -> {
                messages.clear()
                adapter.notifyDataSetChanged()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun restartConversation() {
        Firebase.auth.currentUser?.getIdToken(true)?.addOnSuccessListener { tokenResult ->
            val idToken = tokenResult.token ?: return@addOnSuccessListener

            // The callback now expects ResponseBody
            RetrofitClient.instance.restartChat(idToken).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    // ✅ FIX: Just check if the HTTP status was successful (2xx).
                    // We don't care about the body at all.
                    if (response.isSuccessful) {
                        messages.clear()
                        adapter.notifyDataSetChanged()
                        Toast.makeText(
                            this@AiChatActivity,
                            "Conversation restarted",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(this@AiChatActivity, "Restart failed", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@AiChatActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Firebase.auth.currentUser?.getIdToken(true)?.addOnSuccessListener { tokenResult ->
            val idToken = tokenResult.token ?: return@addOnSuccessListener
            RetrofitClient.instance.restartChat(idToken).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.d("AI_CHAT", "server history cleared on exit")
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.w("AI_CHAT", "failed clearing server history on exit")
                }
            })
        }
    }
}
