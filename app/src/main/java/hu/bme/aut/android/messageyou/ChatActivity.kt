package hu.bme.aut.android.messageyou

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import hu.bme.aut.android.messageyou.adapter.ChatAdapter
import hu.bme.aut.android.messageyou.data.Message
import hu.bme.aut.android.messageyou.databinding.ActivityChatBinding
import hu.bme.aut.android.messageyou.extensions.validateNonEmpty
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.*


class ChatActivity : BaseActivity(), ChatAdapter.MessageItemClickListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityChatBinding
    private lateinit var messagesAdapter: ChatAdapter
    lateinit var partnerEmail: String
    private var imageAdded = false
    lateinit var imageBitmap: Bitmap

    companion object {
        private const val REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        binding = ActivityChatBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        supportActionBar?.hide()

        if(intent.getStringExtra(Message.AUTHOR) == userEmail){
            partnerEmail = intent.getStringExtra(Message.RECIPIENT)!!
        }else{
            partnerEmail = intent.getStringExtra(Message.AUTHOR)!!
        }

        binding.title.text = partnerEmail


        messagesAdapter = ChatAdapter(applicationContext)
        binding.contentChat.rvChat.layoutManager = LinearLayoutManager(this).apply{
            reverseLayout = true
            stackFromEnd = true
        }
        binding.contentChat.rvChat.adapter = messagesAdapter

        binding.btnSend.setOnClickListener { sendClick() }
        binding.btnAttach.setOnClickListener { attachClick() }
        messagesAdapter.itemClickListener = this

        binding.outerScrollView.setOnTouchListener(OnTouchListener { v, event -> true })

        initMessagesListener()
    }

    override fun onBackPressed() {
        startActivity(Intent(this, MessagesActivity::class.java))
    }

    private fun sendClick() {
        if (!validateForm()) {
            return
        }

        if (!imageAdded) {
            uploadMessage()
        } else {
            try {
                uploadPostWithImage()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun validateForm() = binding.etChat.validateNonEmpty()

    private fun uploadMessage(imageUrl: String? = null) {

        val newMessage = Message(uid, userEmail, partnerEmail, binding.etChat.text.toString(), imageUrl)

        val db = Firebase.firestore

        db.collection("messages:" + userEmail)
            .add(newMessage)

        db.collection("messages:" + partnerEmail)
            .add(newMessage)

        imageAdded = false
        binding.etChat.setText("")
    }

    private fun attachClick() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(takePictureIntent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }

        if (requestCode == REQUEST_CODE) {
            imageBitmap = data?.extras?.get("data") as? Bitmap ?: return
            imageAdded = true
        }
    }

    private fun uploadPostWithImage() {
        val bitmap: Bitmap = imageBitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageInBytes = baos.toByteArray()

        val storageReference = FirebaseStorage.getInstance().reference
        val newImageName = URLEncoder.encode(UUID.randomUUID().toString(), "UTF-8") + ".jpg"
        val newImageRef = storageReference.child("images/$newImageName")

        newImageRef.putBytes(imageInBytes)
            .addOnFailureListener { exception ->
                toast(exception.message)
            }
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }

                newImageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                uploadMessage(downloadUri.toString())
            }
    }

    private fun notifyDataSetChanged(){
        messagesAdapter.notifyDataSetChanged()
        binding.contentChat.rvChat.post(Runnable {
            binding.contentChat.rvChat.smoothScrollToPosition(messagesAdapter.itemCount - 1)
        })
    }


    private fun initMessagesListener(){
        val db = Firebase.firestore
        db.collection("messages:" + userEmail).addSnapshotListener { snapshots, e ->
            if(e != null){
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            for (dc in snapshots!!.documentChanges) {
                when(dc.type) {
                    DocumentChange.Type.ADDED -> {
                        val message = dc.document.toObject<Message>()
                        message.id = dc.document.id
                        if(message.author == partnerEmail || message.recipient == partnerEmail && message.recipient != message.author) {
                            messagesAdapter.addMessage(message)
                            notifyDataSetChanged()
                        }
                    }
                    DocumentChange.Type.MODIFIED -> Toast.makeText(this, dc.document.data.toString(), Toast.LENGTH_SHORT).show()
                    DocumentChange.Type.REMOVED -> {
                        val message = dc.document.toObject<Message>()
                        messagesAdapter.removeMessage(message)
                        notifyDataSetChanged()
                    }

                }

            }
        }
    }


    override fun onItemClick(message: Message) {
        val popupMenu: PopupMenu = PopupMenu(this, binding.toolbar)
        popupMenu.inflate(R.menu.options_menu)
        popupMenu.setOnMenuItemClickListener {
            if(it.itemId == R.id.delete){
                messagesAdapter.removeMessage(message)
                val db = Firebase.firestore
                db.collection("messages:" + userEmail).document(message.id!!).delete()
            }
            false
        }
        popupMenu.show()
    }


}