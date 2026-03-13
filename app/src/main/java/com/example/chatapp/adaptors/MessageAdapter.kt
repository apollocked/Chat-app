package com.example.chatapp.adaptors

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.model.ChatMessage
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

class MessageAdapter(
    private val context: Context,
    private val messageList: List<ChatMessage>,
    private val messageIds: List<String>,
    private var userMap: Map<String, User>,
    private val onProfileClick: (User) -> Unit,
    private val onOwnProfileClick: () -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2

    fun updateUsers(newMap: Map<String, User>) {
        userMap = newMap
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        return if (message.user?.uid == FirebaseAuth.getInstance().currentUser?.uid) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == VIEW_TYPE_SENT) {
            R.layout.item_message_sent
        } else {
            R.layout.item_message
        }
        val view = LayoutInflater.from(context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val chatMessage = messageList[position]
        val messageId = messageIds[position]
        
        val uid = chatMessage.user?.uid ?: ""
        val latestUser = userMap[uid] ?: chatMessage.user

        holder.nameTv.text = latestUser?.name ?: "Unknown"

        if (chatMessage.messageText.isNotEmpty()) {
            holder.messageTv.visibility = View.VISIBLE
            holder.messageTv.text = chatMessage.messageText
        } else {
            holder.messageTv.visibility = View.GONE
        }

        if (!chatMessage.messageImage.isNullOrEmpty()) {
            holder.messageIv.visibility = View.VISIBLE
            if (chatMessage.messageImage == "PENDING") {
                holder.messageIv.setImageResource(R.drawable.group)
                holder.messageIv.alpha = 0.5f
            } else {
                holder.messageIv.alpha = 1.0f
                try {
                    val imageBytes = Base64.decode(chatMessage.messageImage, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    holder.messageIv.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    holder.messageIv.visibility = View.GONE
                }
            }
        } else {
            holder.messageIv.visibility = View.GONE
        }

        chatMessage.timestamp?.let {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            holder.timeTv.text = sdf.format(it.toDate())
        }

        val base64Image = latestUser?.profileImage ?: ""
        if (base64Image.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.profileIv.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.profileIv.setImageResource(R.drawable.profile)
            }
        } else {
            holder.profileIv.setImageResource(R.drawable.profile)
        }

        // Tap on profile image
        holder.profileIv.setOnClickListener {
            if (getItemViewType(position) == VIEW_TYPE_SENT) {
                onOwnProfileClick()
            } else {
                latestUser?.let { onProfileClick(it) }
            }
        }

        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            holder.itemView.setOnLongClickListener {
                showDeleteDialog(messageId)
                true
            }
        }
    }

    private fun showDeleteDialog(messageId: String) {
        AlertDialog.Builder(context)
            .setTitle("Unsend Message")
            .setMessage("Are you sure you want to Unsend this message?")
            .setPositiveButton("Unsend") { _, _ ->
                onDeleteClick(messageId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun getItemCount(): Int = messageList.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileIv: ImageView = itemView.findViewById(R.id.itemProfileImage)
        val nameTv: TextView = itemView.findViewById(R.id.itemNameTv)
        val messageTv: TextView = itemView.findViewById(R.id.itemMessageTv)
        val messageIv: ImageView = itemView.findViewById(R.id.itemMessageIv)
        val timeTv: TextView = itemView.findViewById(R.id.itemTimeTv)
    }
}