package com.example.chatapp.adaptors

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.model.ChatMessage

class MessageAdapter(private val context: Context, private val messageList: List<ChatMessage>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        holder.nameTv.text = message.senderName
        holder.messageTv.text = message.messageText

        if (message.senderImage.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(message.senderImage, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.profileIv.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.profileIv.setImageResource(R.drawable.profile)
            }
        } else {
            holder.profileIv.setImageResource(R.drawable.profile)
        }
    }

    override fun getItemCount(): Int = messageList.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileIv: ImageView = itemView.findViewById(R.id.itemProfileImage)
        val nameTv: TextView = itemView.findViewById(R.id.itemNameTv)
        val messageTv: TextView = itemView.findViewById(R.id.itemMessageTv)
    }
}