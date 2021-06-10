package hu.bme.aut.android.messageyou

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import hu.bme.aut.android.messageyou.adapter.MessagesAdapter
import hu.bme.aut.android.messageyou.data.Message
import hu.bme.aut.android.messageyou.databinding.ActivityMessagesBinding
import hu.bme.aut.android.messageyou.services.NotificationService
import java.util.*

class MessagesActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener, MessagesAdapter.MessageItemClickListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMessagesBinding
    private lateinit var messagesAdapter: MessagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.startService(Intent(this, NotificationService::class.java))
        binding = ActivityMessagesBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMessages.toolbar)

        binding.appBarMessages.fab.setOnClickListener {
            val createMessageIntent = Intent(this, CreateMessageActivity::class.java)
            startActivity(createMessageIntent)
        }

        binding.navView.setNavigationItemSelectedListener(this)

        messagesAdapter = MessagesAdapter(applicationContext)
        binding.appBarMessages.contentMessages.rvMessages.layoutManager = LinearLayoutManager(this).apply{
            reverseLayout = true
            stackFromEnd = true
        }
        binding.appBarMessages.contentMessages.rvMessages.adapter = messagesAdapter
        messagesAdapter.itemClickListener = this
        binding.navView.getHeaderView(0).findViewById<TextView>(R.id.userName).text = userEmail

        initMessagesListener()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun notifyDataSetChanged(){
        messagesAdapter.notifyDataSetChanged()
        binding.appBarMessages.contentMessages.rvMessages.post(Runnable {
            binding.appBarMessages.contentMessages.rvMessages.smoothScrollToPosition(messagesAdapter.itemCount - 1)
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
                        val emailToCheck: String?
                        emailToCheck = if(message.author == userEmail){
                            message.recipient
                        } else{
                            message.author
                        }
                        val latestMessage = messagesAdapter.getLatestMessageByEmail(emailToCheck)
                        if(latestMessage == null){
                            messagesAdapter.addMessage(message)
                        }
                        else if(latestMessage.date < message.date){
                            messagesAdapter.removeMessage(latestMessage)
                            messagesAdapter.addMessage(message)
                        }
                        notifyDataSetChanged()
                    }
                    DocumentChange.Type.MODIFIED -> Toast.makeText(this, dc.document.data.toString(), Toast.LENGTH_SHORT).show()
                    DocumentChange.Type.REMOVED -> {
                        notifyDataSetChanged()
                    }
                }
            }
        }
    }

    override fun onItemClick(message: Message) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra(Message.AUTHOR, message.author)
        intent.putExtra(Message.RECIPIENT, message.recipient)
        startActivity(intent)
    }

}