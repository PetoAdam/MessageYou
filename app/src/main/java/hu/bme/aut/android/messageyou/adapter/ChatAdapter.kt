package hu.bme.aut.android.messageyou.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import hu.bme.aut.android.messageyou.data.Message
import hu.bme.aut.android.messageyou.databinding.CardChatBinding

class ChatAdapter(private val context: Context) : ListAdapter<Message, ChatAdapter.ChatViewHolder>(itemCallback){

    private val messageList: MutableList<Message> = mutableListOf()
    private var lastPosition = -1
    var itemClickListener: MessageItemClickListener? = null


    inner class ChatViewHolder(binding: CardChatBinding) : RecyclerView.ViewHolder(binding.root){
        val tvText: TextView = binding.tvText
        val imgPost: ImageView = binding.imgPost
        var message: Message? = null
        val cardView = binding.cardView

        init{
            itemView.setOnClickListener {
                message?.let{ itemClickListener?.onItemClick(it)}
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder =
        ChatViewHolder(
            CardChatBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val tmpMessage = messageList[position]
        if (tmpMessage.imageUrl.isNullOrBlank()) {
            holder.imgPost.visibility = View.GONE
        } else {
            Glide.with(context).load(tmpMessage.imageUrl).into(holder.imgPost)
            holder.imgPost.visibility = View.VISIBLE
        }
        var params = holder.cardView.layoutParams as FrameLayout.LayoutParams
        if(FirebaseAuth.getInstance().currentUser.email == tmpMessage.recipient){
            holder.tvText.text = tmpMessage.text
            holder.cardView.setBackgroundColor(Color.parseColor("#FF424242"))
            holder.tvText.setTextColor(Color.WHITE)
            params.gravity = Gravity.LEFT
        }
        else{
            holder.tvText.text = tmpMessage.text
            holder.cardView.setBackgroundColor(Color.parseColor("#1daf92"))
            holder.tvText.setTextColor(Color.BLACK)
            params.gravity = Gravity.RIGHT
        }

        holder.message = tmpMessage

        setAnimation(holder.itemView, position)
    }

    fun addMessage(message: Message?) {
        message ?: return

        messageList += (message)
        messageList.sortBy { it.date }
        submitList((messageList))
        notifyDataSetChanged()
    }

    private fun setAnimation(viewToAnimate: View, position: Int) {
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left)
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }

    fun containsChat(email: String?): Boolean{
        messageList.forEach{
            if(it.author == email || it.recipient == email)
                return true
        }
        return false
    }

    fun removeMessage(message: Message?): Boolean{
        message ?: return false
        messageList.remove(message)
        notifyDataSetChanged()
        return true
    }

    interface MessageItemClickListener{
        fun onItemClick(message: Message)
    }

    companion object {
        object itemCallback : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
                return oldItem == newItem
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
                return oldItem == newItem
            }
        }
    }
}