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

    private val VIEW_TYPE_ME = 1
    private val VIEW_TYPE_OTHER = 2

    // This method determines which layout to use based on the sender
    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.isMine) {
            VIEW_TYPE_ME
        } else {
            VIEW_TYPE_OTHER
        }
    }

    // This method inflates the correct layout based on the view type
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layout = if (viewType == VIEW_TYPE_ME) {
            R.layout.item_chat_message_me // Layout for your messages
        } else {
            R.layout.item_chat_message // Layout for messages from others
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ChatViewHolder(view)
    }

    override fun getItemCount(): Int = messages.size

    // This method binds the message data to the views
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val msg = messages[position]
        holder.messageText.text = msg.message
        holder.timestampText.text = SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(Date(msg.timestamp))

        // Set the sender's name based on whether it's your message or someone else's
        if (holder.itemViewType == VIEW_TYPE_ME) {
            holder.senderText.text = "You"
        } else {
            holder.senderText.text = msg.senderName
        }
    }

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.chatMessage)
        val senderText: TextView = view.findViewById(R.id.chatSender)
        val timestampText: TextView = view.findViewById(R.id.chatTimestamp)
    }
}