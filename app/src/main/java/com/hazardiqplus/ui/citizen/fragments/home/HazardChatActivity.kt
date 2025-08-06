package com.hazardiqplus.ui.citizen.fragments.home

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
import org.json.JSONObject
import java.util.*

class HazardChatActivity : AppCompatActivity() {

    private lateinit var hazardTitle: TextView
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var adapter: ChatMessageAdapter

    private val messages = ArrayList<ChatMessage>()
    private lateinit var socket: Socket
    private lateinit var hazardId: String
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hazard_chat)

        hazardTitle = findViewById(R.id.hazardTitle)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)

        firebaseAuth = Firebase.auth

        val hazardType = intent.getStringExtra("hazard_type") ?: "Hazard"
        val hazardIdLong = intent.getLongExtra("hazard_id", -1L)

        if (hazardIdLong == -1L) {
            Log.e("HazardChat", "Missing or invalid hazard_id in intent")
            finish()
            return
        }

        hazardId = hazardIdLong.toString() // for use in socket room
        hazardTitle.text = "$hazardType | ID: $hazardId"


        adapter = ChatMessageAdapter(messages)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = adapter

        connectSocket()

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
            socket.connect()

            socket.on(Socket.EVENT_CONNECT) {
                Log.d("SOCKET", "✅ Connected to socket")
                joinHazardRoom()
            }

            socket.on("joinedRoom") {
                Log.d("SOCKET", "✅ Joined hazard room")
            }

            socket.on("authError") { args ->
                runOnUiThread {
                    Log.e("SOCKET", "❌ Auth failed: ${args[0]}")
                }
            }

            socket.on("newMessage", onNewMessage)

            socket.on(Socket.EVENT_DISCONNECT) {
                Log.d("SOCKET", "🔌 Disconnected from socket")
            }

        } catch (e: Exception) {
            Log.e("SOCKET", "❌ Error connecting to socket", e)
        }
    }

    private fun joinHazardRoom() {
        firebaseAuth.currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val token = result.token
            val joinData = JSONObject()
            joinData.put("hazardId", hazardId)
            joinData.put("firebaseToken", token)
            socket.emit("joinHazardRoom", joinData)
        }?.addOnFailureListener {
            Log.e("SOCKET", "❌ Failed to get token: ${it.message}")
        }
    }

    private fun sendMessage(messageText: String) {
        firebaseAuth.currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val token = result.token
            val data = JSONObject()
            data.put("hazardId", hazardId)
            data.put("firebaseToken", token)
            data.put("message", messageText)
            socket.emit("sendMessage", data)
            Log.d("SOCKET", "📤 Sent message: $messageText")
            messageInput.text.clear()
        }
    }

    private val onNewMessage = Emitter.Listener { args ->
        if (args.isNotEmpty()) {
            val data = args[0] as JSONObject
            val message = data.getString("message")
            val sender = data.getString("senderUid")
            val timestamp = data.getLong("timestamp")

            val chatMessage = ChatMessage(message = message, sender = sender, timestamp = timestamp)

            runOnUiThread {
                messages.add(chatMessage)
                adapter.notifyItemInserted(messages.size - 1)
                chatRecyclerView.scrollToPosition(messages.size - 1)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.disconnect()
        socket.off("newMessage", onNewMessage)
    }
}
