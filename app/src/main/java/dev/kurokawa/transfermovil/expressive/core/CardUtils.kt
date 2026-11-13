package dev.kurokawa.transfermovil.expressive.core

import dev.kurokawa.transfermovil.expressive.model.Bank

/** Kotlin mirror of libtm_c (Luhn, BIN→bank, amount parse). The C core
 *  is authoritative; this mirror keeps the UI working in v1.0 before
 *  the NDK link, exactly like the SMS parser twin. */
object CardUtils {
    fun luhnValid(pan: String): Boolean {
        val d = pan.filter { it.isDigit() }
        if (d.length !in 13..19) return false
        var sum = 0; var dbl = false
        for (i in d.indices.reversed()) {
            var n = d[i] - '0'
            if (dbl) { n *= 2; if (n > 9) n -= 9 }
            sum += n; dbl = !dbl
        }
        return sum % 10 == 0
    }
    fun bankFromBin(pan: String): Bank? {
        val d = pan.filter { it.isDigit() }
        return when (d.take(4)) {
            "9238", "9204" -> Bank.BPA
            "9224", "9243" -> Bank.BANDEC
            "9225", "9205" -> Bank.BANMET
            else -> null
        }
    }
    fun parseAmount(text: String): Double? {
        val m = Regex("""[\d][\d,]*(\.\d+)?""").find(text.trim()) ?: return null
        return m.value.replace(",", "").toDoubleOrNull()
    }
    /** PIN rule: exactly pinLength digits; input fields also cap length. */
    fun isValidPin(pin: String, pinLength: Int): Boolean =
        pin.length == pinLength && pin.all { it.isDigit() }
}
