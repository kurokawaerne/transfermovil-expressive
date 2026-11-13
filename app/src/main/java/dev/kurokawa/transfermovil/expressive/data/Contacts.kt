package dev.kurokawa.transfermovil.expressive.data

import android.content.Context
import android.provider.ContactsContract

data class ContactInfo(val name: String, val number: String, val photoUri: String?)

/** Contacts via ContactsContract, with photo URIs (favourites/avatars). */
object Contacts {
    fun read(ctx: Context): List<ContactInfo> = try {
        val out = mutableListOf<ContactInfo>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        ctx.contentResolver.query(uri,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI),
            null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")?.use { c ->
            val seen = mutableSetOf<String>()
            while (c.moveToNext()) {
                val num = c.getString(1) ?: continue
                if (seen.add(num)) out += ContactInfo(c.getString(0) ?: "", num, c.getString(2))
            }
        }
        out
    } catch (_: Exception) { emptyList() }
}
