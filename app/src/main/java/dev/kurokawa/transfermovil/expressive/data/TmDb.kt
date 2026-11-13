package dev.kurokawa.transfermovil.expressive.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import dev.kurokawa.transfermovil.expressive.core.SmsParser
import dev.kurokawa.transfermovil.expressive.core.SmsRecord
import dev.kurokawa.transfermovil.expressive.model.Bank
import dev.kurokawa.transfermovil.expressive.model.BankCard

data class Favourite(val id: Long = 0, val kind: String, val name: String, val value: String, val photoUri: String? = null)
data class Factura(val id: Long = 0, val kind: String, val code: String, val alias: String = "")
data class StoredSms(val id: Long = 0, val receivedAt: Long, val raw: String, val record: SmsRecord)

/** Local DB: Android SQLite via SQLiteOpenHelper (no Room, per ETK rule). */
class TmDb(ctx: Context) : SQLiteOpenHelper(ctx, "tmex.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE cards(id INTEGER PRIMARY KEY AUTOINCREMENT, bank TEXT, pan TEXT, alias TEXT, currency TEXT, is_default INTEGER, associated INTEGER)")
        db.execSQL("CREATE TABLE favourites(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT, name TEXT, value TEXT, photo_uri TEXT)")
        db.execSQL("CREATE TABLE facturas(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT, code TEXT, alias TEXT)")
        db.execSQL("CREATE TABLE sms(id INTEGER PRIMARY KEY AUTOINCREMENT, received_at INTEGER, raw TEXT, kind TEXT)")
        db.execSQL("CREATE TABLE history(id INTEGER PRIMARY KEY AUTOINCREMENT, at INTEGER, title TEXT, detail TEXT)")
    }
    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {}

    fun cards(): List<BankCard> = readableDatabase.query("cards", null, null, null, null, null, "bank, id").use { c ->
        buildList { while (c.moveToNext()) add(BankCard(c.getLong(0), Bank.fromCode(c.getString(1)), c.getString(2), c.getString(3) ?: "", c.getString(4) ?: "CUP", c.getInt(5) == 1, c.getInt(6) == 1)) }
    }
    fun addCard(card: BankCard): Long = writableDatabase.insert("cards", null, ContentValues().apply {
        put("bank", card.bank.code); put("pan", card.pan); put("alias", card.alias)
        put("currency", card.currency); put("is_default", if (card.isDefault) 1 else 0); put("associated", if (card.associated) 1 else 0)
    })
    fun deleteCard(id: Long) { writableDatabase.delete("cards", "id=?", arrayOf(id.toString())) }

    fun favourites(kind: String? = null): List<Favourite> {
        val sel = if (kind == null) null else "kind=?"
        val args = if (kind == null) null else arrayOf(kind)
        return readableDatabase.query("favourites", null, sel, args, null, null, "name").use { c ->
            buildList { while (c.moveToNext()) add(Favourite(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4))) }
        }
    }
    fun addFavourite(f: Favourite): Long = writableDatabase.insert("favourites", null, ContentValues().apply {
        put("kind", f.kind); put("name", f.name); put("value", f.value); put("photo_uri", f.photoUri)
    })
    fun deleteFavourite(id: Long) { writableDatabase.delete("favourites", "id=?", arrayOf(id.toString())) }

    fun facturas(): List<Factura> = readableDatabase.query("facturas", null, null, null, null, null, "kind, code").use { c ->
        buildList { while (c.moveToNext()) add(Factura(c.getLong(0), c.getString(1), c.getString(2), c.getString(3) ?: "")) }
    }
    fun addFactura(f: Factura): Long = writableDatabase.insert("facturas", null, ContentValues().apply {
        put("kind", f.kind); put("code", f.code); put("alias", f.alias)
    })
    fun deleteFactura(id: Long) { writableDatabase.delete("facturas", "id=?", arrayOf(id.toString())) }

    fun addSms(raw: String, kind: String, at: Long = System.currentTimeMillis()): Long =
        writableDatabase.insert("sms", null, ContentValues().apply { put("received_at", at); put("raw", raw); put("kind", kind) })
    fun smsInbox(): List<StoredSms> = readableDatabase.query("sms", null, null, null, null, null, "received_at DESC").use { c ->
        buildList { while (c.moveToNext()) { val raw = c.getString(2); add(StoredSms(c.getLong(0), c.getLong(1), raw, SmsParser.parse(raw))) } }
    }
    fun addHistory(title: String, detail: String) {
        writableDatabase.insert("history", null, ContentValues().apply { put("at", System.currentTimeMillis()); put("title", title); put("detail", detail) })
    }
    fun history(): List<Pair<String, String>> = readableDatabase.query("history", arrayOf("title", "detail"), null, null, null, null, "at DESC", "50").use { c ->
        buildList { while (c.moveToNext()) add(c.getString(0) to (c.getString(1) ?: "")) }
    }
}
