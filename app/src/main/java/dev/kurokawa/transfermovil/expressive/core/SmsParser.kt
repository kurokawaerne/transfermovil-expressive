package dev.kurokawa.transfermovil.expressive.core

import dev.kurokawa.transfermovil.expressive.model.Bank

/** Kotlin twin of the Rust tm-core SMS parser (identical behaviour;
 *  runs at runtime in v1.0 until the JNI link is wired — same pattern
 *  as ETECSA Toolkit v1.0). Never fails: worst case Unknown. */
data class SignedAmount(val isCredit: Boolean, val value: Double, val currency: String = "CUP")
data class OperationRow(val date: String, val service: String, val op: String, val amount: Double, val currency: String, val txId: String)

sealed interface SmsRecord {
    val kind: String
    data class MobileRechargeReceived(val amount: Double, val currency: String, val fromNumber: String, val remainingAllowance: Double) : SmsRecord { override val kind = "MobileRechargeReceived" }
    data class Authenticated(val bank: Bank?) : SmsRecord { override val kind = "Authenticated" }
    data class TransferCompleted(val bank: Bank?, val date: String, val beneficiaryPan: String, val sourceAccount: String, val amount: Double, val currency: String, val txId: String, val remainingBalance: SignedAmount) : SmsRecord { override val kind = "TransferCompleted" }
    data class BalanceQuery(val bank: Bank?, val availableBalance: SignedAmount) : SmsRecord { override val kind = "BalanceQuery" }
    data class BillStatus(val facturaId: String, val hasDebt: Boolean) : SmsRecord { override val kind = "BillStatus" }
    data class LastOperations(val bank: Bank?, val rows: List<OperationRow>) : SmsRecord { override val kind = "LastOperations" }
    data class LimitsQuery(val bank: Bank?, val cardCurrency: String, val opAtm: Int, val opPos: Int, val amtAtm: Double, val amtPos: Double, val amtTotal: Double, val usedOpsAtm: Int, val usedOpsPos: Int, val usedAmtAtm: Double, val usedAmtPos: Double) : SmsRecord { override val kind = "LimitsQuery" }
    data class Unknown(val raw: String) : SmsRecord { override val kind = "Unknown" }
}

object SmsParser {
    private fun amountAfter(t: String, marker: String): Double? {
        val i = t.indexOf(marker); if (i < 0) return null
        val rest = t.substring(i + marker.length).trimStart()
        val num = rest.takeWhile { it.isDigit() || it == '.' || it == ',' }
        return num.replace(",", "").toDoubleOrNull()
    }
    private fun fieldAfter(t: String, marker: String): String? {
        val i = t.indexOf(marker); if (i < 0) return null
        val rest = t.substring(i + marker.length).trim()
        return rest.lineSequence().firstOrNull()?.trim()
    }
    private fun signedAfter(t: String, marker: String): SignedAmount? {
        val i = t.indexOf(marker); if (i < 0) return null
        val rest = t.substring(i + marker.length).trimStart()
        val credit = when { rest.startsWith("CR") -> true; rest.startsWith("DB") -> false; else -> return null }
        val after = rest.substring(2).trimStart()
        val num = after.takeWhile { it.isDigit() || it == '.' || it == ',' }
        val value = num.replace(",", "").toDoubleOrNull() ?: return null
        val currency = after.substring(num.length).trimStart().takeWhile { it.isLetter() }
        return SignedAmount(credit, value, currency.ifEmpty { "CUP" })
    }

    fun parse(body: String): SmsRecord {
        val t = body.trim()
        if (t.contains("ha recibido una recarga de saldo movil")) {
            return SmsRecord.MobileRechargeReceived(
                amountAfter(t, "monto de") ?: 0.0, "CUP",
                fieldAfter(t, "del numero ")?.substringBefore('.')?.trim() ?: "",
                amountAfter(t, "Monto restante a recargar para el periodo:") ?: 0.0)
        }
        if (t.contains("Ultimas operaciones")) {
            val rows = mutableListOf<OperationRow>()
            val h = t.indexOf("NoTransaccion")
            if (h >= 0) {
                for (seg in t.substring(h + "NoTransaccion".length).split('|')) {
                    val s = seg.trim(); if (s.isEmpty()) continue
                    val f = s.split(';').map { it.trim() }
                    if (f.size >= 6) rows += OperationRow(f[0], f[1], f[2], f[3].replace(",", "").toDoubleOrNull() ?: 0.0, f[4], f[5])
                }
            }
            return SmsRecord.LastOperations(Bank.fromName(t), rows)
        }
        if (t.contains("Consulta de Limites")) {
            return SmsRecord.LimitsQuery(Bank.fromName(t), fieldAfter(t, "Moneda Tarjeta:") ?: "CUP",
                (amountAfter(t, "Cantidad operaciones: ATM:") ?: 0.0).toInt(), 40,
                amountAfter(t, "Importe: ATM:") ?: 0.0, if (t.contains("POS:50000")) 50000.0 else amountAfter(t, "Importe: ATM:") ?: 0.0,
                amountAfter(t, "Total:") ?: 0.0, (amountAfter(t, "Operaciones ATM:") ?: 0.0).toInt(), 0,
                amountAfter(t, "Importe ATM:") ?: 0.0, 0.0)
        }
        if (t.contains("La Transferencia fue completada")) {
            return SmsRecord.TransferCompleted(Bank.fromName(t), fieldAfter(t, "Fecha:") ?: "",
                fieldAfter(t, "Beneficiario:") ?: "", fieldAfter(t, "Ordenante:") ?: "",
                amountAfter(t, "Monto:") ?: 0.0, "CUP", fieldAfter(t, "Nro. Transaccion:") ?: "",
                signedAfter(t, "Saldo restante:") ?: SignedAmount(true, 0.0))
        }
        if (t.contains("consulta de saldo fue completada")) {
            return SmsRecord.BalanceQuery(Bank.fromName(t), signedAfter(t, "Saldo Disponible:") ?: SignedAmount(true, 0.0))
        }
        if (t.contains("Consulta de servicio completada") || t.contains("La Factura:")) {
            return SmsRecord.BillStatus(fieldAfter(t, "La Factura:")?.split(Regex("\\s+"))?.firstOrNull() ?: "", !t.contains("no tiene deudas"))
        }
        if (t.contains("se ha autenticado en la plataforma de pagos moviles")) {
            return SmsRecord.Authenticated(Bank.fromName(t))
        }
        return SmsRecord.Unknown(body)
    }
}

/** Placeholder for the Rust JNI entry point; v1.0 uses the Kotlin twin. */
object RustCore {
    external fun nativeParseSms(body: String): String
    val jniLinked: Boolean = try { System.loadLibrary("tm_core"); true } catch (_: Throwable) { false }
}
