package hu.bme.aut.android.messageyou.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import hu.bme.aut.android.messageyou.MessagesActivity
import hu.bme.aut.android.messageyou.R
import hu.bme.aut.android.messageyou.data.Message
import java.util.*

class NotificationService : Service() {

    companion object{
        private const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "NotificationChannel"
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        createNotificationChannel()
        initMessagesListener()
        return Service.START_STICKY
    }

    private fun sendNotification(author: String?, text: String?){

        val intent = Intent(this, MessagesActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle(author)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        with(NotificationManagerCompat.from(this)){
            notify(NOTIFICATION_ID, builder.build())
        }

    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun initMessagesListener(){
        val currentDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time
        val db = Firebase.firestore
        db.collection("messages:" + FirebaseAuth.getInstance().currentUser.email).addSnapshotListener { snapshots, e ->
            if(e != null){
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            for (dc in snapshots!!.documentChanges) {
                when(dc.type) {
                    DocumentChange.Type.ADDED -> {
                        val message = dc.document.toObject<Message>()
                        if(message.author != FirebaseAuth.getInstance().currentUser.email && message.date > currentDate){
                            sendNotification(message.author, message.text)
                        }
                    }
                    DocumentChange.Type.MODIFIED -> Toast.makeText(this, dc.document.data.toString(), Toast.LENGTH_SHORT).show()
                    DocumentChange.Type.REMOVED -> {
                    }

                }

            }
        }
    }

}