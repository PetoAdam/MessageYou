package hu.bme.aut.android.messageyou

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import hu.bme.aut.android.messageyou.data.Message
import hu.bme.aut.android.messageyou.databinding.ActivityCreateMessageBinding
import hu.bme.aut.android.messageyou.extensions.validateNonEmpty
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.*

class CreateMessageActivity : BaseActivity() {

    companion object {
        private const val REQUEST_CODE = 101
    }

    private lateinit var binding: ActivityCreateMessageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateMessageBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        binding.btnSend.setOnClickListener { sendClick() }
        binding.btnAttach.setOnClickListener { attachClick() }
    }

    private fun sendClick() {
        if (!validateForm()) {
            return
        }

        if (binding.imgAttach.visibility != View.VISIBLE) {
            uploadMessage()
        } else {
            try {
                uploadPostWithImage()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun validateForm() = binding.etRecipient.validateNonEmpty() && binding.etText.validateNonEmpty()

    private fun uploadMessage(imageUrl: String? = null) {

        if(userEmail == binding.etRecipient.text.toString()){
            Toast.makeText(this, "Cannot send a message to yourself", Toast.LENGTH_SHORT).show()
            return
        }
        val newMessage = Message(uid, userEmail, binding.etRecipient.text.toString(), binding.etText.text.toString(), imageUrl)

        val db = Firebase.firestore

        db.collection("messages:" + userEmail)
            .add(newMessage)

        db.collection("messages:" + binding.etRecipient.text.toString())
            .add(newMessage)
            .addOnSuccessListener {
                toast("Message sent")
                finish() }
            .addOnFailureListener { e -> toast(e.toString()) }
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
            val imageBitmap = data?.extras?.get("data") as? Bitmap ?: return
            binding.imgAttach.setImageBitmap(imageBitmap)
            binding.imgAttach.visibility = View.VISIBLE
        }
    }

    private fun uploadPostWithImage() {
        val bitmap: Bitmap = (binding.imgAttach.drawable as BitmapDrawable).bitmap
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

}