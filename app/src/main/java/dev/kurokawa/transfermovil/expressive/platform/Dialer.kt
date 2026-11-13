package dev.kurokawa.transfermovil.expressive.platform

import android.content.Context
import android.content.Intent
import android.net.Uri

/** USSD execution: ACTION_CALL with Uri.encode("#"), exactly like the
 *  original. Falls back to ACTION_DIAL without the CALL permission. */
object Dialer {
    fun dialUssd(ctx: Context, code: String) {
        val uri = Uri.parse("tel:" + Uri.encode(code))
        try {
            ctx.startActivity(Intent(Intent.ACTION_CALL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            try { ctx.startActivity(Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
        }
    }
    fun callNumber(ctx: Context, number: String) = dialUssd(ctx, number)

    /** Opens the system dialer pre-filled with [number] (ACTION_DIAL,
     *  tel: URI): no CALL_PHONE permission needed and the user confirms
     *  the call — used for the emergency numbers in Teléfonos útiles. */
    fun dialNumber(ctx: Context, number: String) {
        val uri = Uri.parse("tel:" + number.filter { it.isDigit() || it == '+' })
        try {
            ctx.startActivity(Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
    }
}
