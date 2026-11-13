package dev.kurokawa.transfermovil.expressive.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dev.kurokawa.transfermovil.expressive.core.SmsParser
import dev.kurokawa.transfermovil.expressive.data.TmDb

/** BroadcastReceiver for sender PAGOxMOVIL: parses, stores and bubbles
 *  the structured result instead of the original's raw alert dialog. */
class PagoSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val sender = messages.firstOrNull()?.originatingAddress ?: return
        if (!sender.equals("PAGOxMOVIL", ignoreCase = true)) return
        val body = messages.joinToString("") { it.messageBody ?: "" }
        if (body.isBlank()) return
        val record = SmsParser.parse(body)
        TmDb(context).addSms(body, record.kind)
        SmsNotifier.notifyParsed(context, record, "PAGOxMOVIL · ${record.kind}", body.take(120))
    }
}

/** Inbox scan: reads stored PAGOxMOVIL messages (READ_SMS) for backfill. */
object PagoInbox {
    fun scan(context: Context): Int {
        var added = 0
        try {
            val uri = Telephony.Sms.Inbox.CONTENT_URI
            context.contentResolver.query(uri, arrayOf("address", "body", "date"),
                "address LIKE ?", arrayOf("%PAGOxMOVIL%"), "date DESC")?.use { c ->
                val db = TmDb(context)
                val known = db.smsInbox().map { it.raw }.toSet()
                while (c.moveToNext()) {
                    val body = c.getString(1) ?: continue
                    if (body in known) continue
                    db.addSms(body, SmsParser.parse(body).kind, c.getLong(2)); added++
                }
            }
        } catch (_: Exception) {}
        return added
    }
}
