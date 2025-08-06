package com.hazardiqplus.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hazardiqplus.R
import com.hazardiqplus.data.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

class ChatMessageAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatMessageAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val msg = messages[position]
        holder.messageText.text = msg.message
        holder.senderText.text = msg.sender
        holder.timestampText.text = SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(Date(msg.timestamp))
    }

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.chatMessage)
        val senderText: TextView = view.findViewById(R.id.chatSender)
        val timestampText: TextView = view.findViewById(R.id.chatTimestamp)
    }
}
