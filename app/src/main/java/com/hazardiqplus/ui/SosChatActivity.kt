package com.hazardiqplus.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.hazardiqplus.R
import com.hazardiqplus.adapters.ChatMessageAdapter
import com.hazardiqplus.data.ChatMessage
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject
import kotlin.properties.Delegates

class SosChatActivity : AppCompatActivity() {

    private lateinit var sosTitle: TextView
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var adapter: ChatMessageAdapter

    private val messages = ArrayList<ChatMessage>()
    private var socket: Socket? = null
    private var sosId by Delegates.notNull<Int>()
    private lateinit var firebaseAuth: FirebaseAuth
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hazard_chat)

        firebaseAuth = Firebase.auth
        currentUserId = firebaseAuth.currentUser?.uid

        sosTitle = findViewById(R.id.hazardTitle)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)

        val receivedSosId = intent.getStringExtra("sosId")
        if (receivedSosId == null) {
            Log.e("SosChat", "❌ Missing sos_id in intent")
            finish()
            return
        }
        sosId = receivedSosId.toInt()

        sosTitle.text = "🚨 SOS Chat | ID: $sosId"

        adapter = ChatMessageAdapter(messages)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = adapter

        if (currentUserId != null) {
            connectSocket()
        } else {
            Log.e("SosChat", "User not logged in.")
        }

        sendButton.setOnClickListener {
            val text = messageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }
    }

    private fun connectSocket() {
        try {
            socket = IO.socket("https://hazardiq-bwxg.onrender.com")

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SOCKET", "✅ Connected to socket")
                joinSosRoom()
            }

            socket?.on("joinedRoom") { Log.d("SOCKET", "✅ Joined SOS room") }
            socket?.on("chatHistory", onChatHistory)
            socket?.on("authError") { args ->
                runOnUiThread {
                    Log.e("SOCKET", "❌ Auth failed: ${args[0]}")
                }
            }
            socket?.on("newMessage", onNewMessage)
            socket?.on(Socket.EVENT_DISCONNECT) { Log.d("SOCKET", "🔌 Disconnected from socket") }

            socket?.connect()

        } catch (e: Exception) {
            Log.e("SOCKET", "❌ Error connecting to socket", e)
        }
    }

    private val onChatHistory = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            val data = args[0] as JSONArray
            runOnUiThread {
                messages.clear()
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    val message = item.getString("message")
                    val senderUid = item.getString("senderUid")
                    val senderName = item.getString("senderName")
                    val timestamp = item.getLong("timestamp")

                    val chatMessage = ChatMessage(
                        message = message,
                        sender = senderUid,
                        senderName = senderName,
                        timestamp = timestamp
                    )
                    chatMessage.isMine = senderUid == currentUserId
                    messages.add(chatMessage)
                }
                adapter.notifyDataSetChanged()
                chatRecyclerView.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun joinSosRoom() {
        firebaseAuth.currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val token = result.token
            val joinData = JSONObject()
            joinData.put("sosId", sosId)
            joinData.put("firebaseToken", token)
            socket?.emit("joinSosRoom", joinData)
        }?.addOnFailureListener {
            Log.e("SOCKET", "❌ Failed to get token: ${it.message}")
        }
    }

    private fun sendMessage(messageText: String) {
        firebaseAuth.currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val token = result.token
            val data = JSONObject()
            data.put("sosId", sosId)
            data.put("firebaseToken", token)
            data.put("message", messageText)
            socket?.emit("sendSosMessage", data)
            Log.d("SOCKET", "📤 Sent message: $messageText")
            messageInput.text.clear()
        }?.addOnFailureListener {
            Log.e("SOCKET", "❌ Failed to get token for sending message: ${it.message}")
        }
    }

    private val onNewMessage = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            val data = args[0] as JSONObject
            val message = data.getString("message")
            val senderUid  = data.getString("senderUid")
            val senderName = data.getString("senderName")
            val timestamp = data.getLong("timestamp")

            val chatMessage = ChatMessage(
                message = message,
                sender = senderUid,
                senderName = senderName,
                timestamp = timestamp
            )
            chatMessage.isMine = senderUid == currentUserId
            runOnUiThread {
                messages.add(chatMessage)
                adapter.notifyItemInserted(messages.size - 1)
                chatRecyclerView.scrollToPosition(messages.size - 1)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socket?.disconnect()
        socket?.close()
    }
}