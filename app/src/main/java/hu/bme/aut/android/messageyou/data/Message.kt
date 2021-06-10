package hu.bme.aut.android.messageyou.data

import java.time.LocalDateTime
import java.util.*


class Message(
    var uid: String? = null,
    var author: String? = null,
    var recipient: String? = null,
    var text: String? = null,
    var imageUrl: String? = null,
    var id: String? = null,
    var date: Date = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time
)
{
    companion object{
        const val AUTHOR = "AUTHOR"
        const val RECIPIENT = "RECIPIENT"
    }
}