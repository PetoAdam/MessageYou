package hu.bme.aut.android.messageyou.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import hu.bme.aut.android.messageyou.BaseActivity
import hu.bme.aut.android.messageyou.data.Message
import hu.bme.aut.android.messageyou.databinding.CardMessageBinding
import java.util.*

class MessagesAdapter(private val context: Context) : ListAdapter<Message, MessagesAdapter.MessageViewHolder>(itemCallback){

    private var messageList: MutableList<Message> = mutableListOf()
    private var lastPosition = -1
    var itemClickListener: MessageItemClickListener? = null


    inner class MessageViewHolder(binding: CardMessageBinding) : RecyclerView.ViewHolder(binding.root){
        val tvAuthor: TextView = binding.tvAuthor
        val tvText: TextView = binding.tvText
        var message: Message? = null

        init{
            itemView.setOnClickListener {
                message?.let{ itemClickListener?.onItemClick(it)}
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder =
        MessageViewHolder(
            CardMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val tmpMessage = messageList[position]
        if(FirebaseAuth.getInstance().currentUser.email == tmpMessage.recipient){
            holder.tvAuthor.text = tmpMessage.author
            holder.tvText.text = tmpMessage.text
        }
        else{
            holder.tvAuthor.text = tmpMessage.recipient
            holder.tvText.text = "You: " + tmpMessage.text
        }

        holder.message = tmpMessage

        setAnimation(holder.itemView, position)
    }

    fun addMessage(message: Message?) {
        message ?: return

        messageList.add(message)
        messageList.sortBy { it.date }
        submitList((messageList))
        notifyDataSetChanged()
    }

    fun removeMessage(message: Message?): Boolean{
        message ?: return false
        messageList.remove(message)
        notifyDataSetChanged()
        return true
    }

    private fun setAnimation(viewToAnimate: View, position: Int) {
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left)
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }


    fun removeMessageByEmail(email: String?){
        var tempList = mutableListOf<Message>()
        messageList.forEach {
            if(!(it.author == email || it.recipient == email)){
                tempList.add(it)
            }
        }
        messageList = tempList
        notifyDataSetChanged()
    }

    fun getLatestMessageByEmail(email: String?): Message?{
        if (messageList.size == 0)
            return null
        var message: Message? = null
        messageList.forEach {
            if(it.author == email || it.recipient == email)
                message = it
        }
        return message
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