package com.example.chatapp.adaptors

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.databinding.ItemMessageBinding
import com.example.chatapp.databinding.ItemMessageSentBinding
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
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            SentMessageViewHolder(ItemMessageSentBinding.inflate(LayoutInflater.from(context), parent, false))
        } else {
            ReceivedMessageViewHolder(ItemMessageBinding.inflate(LayoutInflater.from(context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatMessage = messageList[position]
        val messageId = messageIds[position]
        val uid = chatMessage.user?.uid ?: ""
        val latestUser = userMap[uid] ?: chatMessage.user

        if (holder is SentMessageViewHolder) {
            holder.bind(chatMessage, latestUser, messageId)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(chatMessage, latestUser)
        }
    }

    override fun getItemCount(): Int = messageList.size

    inner class SentMessageViewHolder(val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage, user: User?, messageId: String) {
            binding.itemNameTv.text = user?.name ?: "Me"
            
            if (message.messageText.isNotEmpty()) {
                binding.itemMessageTv.visibility = View.VISIBLE
                binding.itemMessageTv.text = message.messageText
            } else {
                binding.itemMessageTv.visibility = View.GONE
            }

            if (!message.messageImage.isNullOrEmpty()) {
                binding.itemMessageIv.visibility = View.VISIBLE
                loadImage(message.messageImage, binding.itemMessageIv)
            } else {
                binding.itemMessageIv.visibility = View.GONE
            }

            message.timestamp?.let {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                binding.itemTimeTv.text = sdf.format(it.toDate())
            }

            loadImage(user?.profileImage, binding.itemProfileImage, isProfile = true)

            // Read Receipt logic
            when (message.status) {
                1 -> { // Sent
                    binding.itemStatusIv.setImageResource(R.drawable.ic_check)
                    binding.itemStatusIv.setColorFilter(Color.GRAY)
                }
                2 -> { // Read
                    binding.itemStatusIv.setImageResource(R.drawable.ic_done_all)
                    binding.itemStatusIv.setColorFilter(Color.parseColor("#4CAF50")) // Green
                }
                else -> {
                    binding.itemStatusIv.setImageResource(R.drawable.ic_access_time)
                    binding.itemStatusIv.setColorFilter(Color.GRAY)
                }
            }

            binding.itemProfileImage.setOnClickListener { onOwnProfileClick() }
            
            binding.root.setOnLongClickListener {
                showDeleteDialog(messageId)
                true
            }
        }
    }

    inner class ReceivedMessageViewHolder(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage, user: User?) {
            binding.itemNameTv.text = user?.name ?: "Unknown"
            
            if (message.messageText.isNotEmpty()) {
                binding.itemMessageTv.visibility = View.VISIBLE
                binding.itemMessageTv.text = message.messageText
            } else {
                binding.itemMessageTv.visibility = View.GONE
            }

            if (!message.messageImage.isNullOrEmpty()) {
                binding.itemMessageIv.visibility = View.VISIBLE
                loadImage(message.messageImage, binding.itemMessageIv)
            } else {
                binding.itemMessageIv.visibility = View.GONE
            }

            message.timestamp?.let {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                binding.itemTimeTv.text = sdf.format(it.toDate())
            }

            loadImage(user?.profileImage, binding.itemProfileImage, isProfile = true)

            binding.itemProfileImage.setOnClickListener { user?.let { onProfileClick(it) } }
        }
    }

    private fun loadImage(imageData: String?, imageView: android.widget.ImageView, isProfile: Boolean = false) {
        if (imageData.isNullOrEmpty()) {
            if (isProfile) imageView.setImageResource(R.drawable.profile)
            return
        }

        if (imageData.startsWith("http")) {
            Glide.with(context)
                .load(imageData)
                .placeholder(if (isProfile) R.drawable.profile else R.drawable.group)
                .into(imageView)
        } else {
            try {
                val imageBytes = Base64.decode(imageData, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                if (isProfile) imageView.setImageResource(R.drawable.profile)
            }
        }
    }

    private fun showDeleteDialog(messageId: String) {
        AlertDialog.Builder(context)
            .setTitle("Unsend Message")
            .setMessage("Are you sure you want to Unsend this message?")
            .setPositiveButton("Unsend") { _, _ -> onDeleteClick(messageId) }
            .setNegativeButton("Cancel", null)
            .show()
    }
}