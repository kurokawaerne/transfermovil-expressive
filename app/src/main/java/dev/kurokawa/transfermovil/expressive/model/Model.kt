package dev.kurokawa.transfermovil.expressive.model

/** Banks of the *444# platform (assets/agenciasBancarias.json).
 *  pinLength is DATA: every PIN field takes its maxLength from here,
 *  so the BPA 5-digit bug cannot recur — a 5th digit cannot be typed. */
enum class Bank(val code: String, val fullName: String, val pinLength: Int, val brandArgb: Long) {
    BPA("BPA", "Banco Popular de Ahorro", 4, 0xFF0057A8),
    BANDEC("BANDEC", "Banco de Crédito y Comercio", 4, 0xFF8A1538),
    BANMET("BANMET", "Banco Metropolitano", 4, 0xFF003DA5),
    MONEDERO("MONEDERO", "Monedero MiTransfer", 4, 0xFF2CA800);

    companion object {
        fun fromName(name: String): Bank? = when {
            name.contains("Metropolitano") -> BANMET
            name.contains("Popular de Ahorro") -> BPA
            name.contains("Crédito y Comercio") || name.contains("Credito y Comercio") -> BANDEC
            else -> null
        }
        fun fromCode(code: String): Bank = entries.firstOrNull { it.code == code } ?: BPA
    }
}

data class BankCard(
    val id: Long = 0,
    val bank: Bank,
    val pan: String,               // full PAN, stored locally only
    val alias: String = "",
    val currency: String = "CUP",
    val isDefault: Boolean = false,
    val associated: Boolean = false,
) {
    val masked: String get() = CardMask.mask(pan)
}

object CardMask {
    fun mask(pan: String): String {
        val d = pan.filter { it.isDigit() }
        if (d.length < 8) return d
        return d.take(4) + "X".repeat(d.length - 8) + d.takeLast(4)
    }
    fun format(pan: String): String = pan.filter { it.isDigit() }.chunked(4).joinToString(" ")
}

/** Direct-entry USSD map, verbatim from the RE report. Session
 *  operations (transfers, pagos, recargas…) run inside *444# after
 *  authentication and open through the guided step sheet. */
object Ussd {
    const val SESSION = "*444#"
    const val CLOSE_SESSION = "*444*16#"
    const val BALANCE = "*444*46#"
    const val LAST_OPS = "*444*48#"
    const val INTEGRATED = "*444*58#"
    const val LIMITS = "*444*62#"
    const val DELETE_REGISTRATION = "*444*68*02#"
    const val CUBACEL_BALANCE = "*222#"
    const val CUBACEL_PLANS = "*133#"
    const val CUBACEL_SERVICES = "*111#"
    const val ADELANTA = "*234*3#"
    const val CUBACEL_DATA = "*222*328#"
    const val CUBACEL_VOICE = "*222*869#"
    const val CUBACEL_SMS = "*222*767#"
    const val PLAN_AMIGOS = "*222*264#"
    const val CUBACEL_BONUS = "*222*266#"
}

/** Service taxonomy (ARCHITECTURE.md): categories in MD3E. An operation
 *  either dials a direct code or opens a guided session form. */
enum class Category(val title: String) {
    CONSULTAS("Consultas"), TRANSFERENCIAS("Transferencias"), PAGOS("Pagos"),
    RECARGAS("Recargas"), CUBACEL("Cubacel"), MONEDERO("Monedero MiTransfer"),
    RESERVAS("Reservas y Trámites"), AGENTE("Agente"), GESTION("Gestión"),
    UTILIDADES("Utilidades"),
}

data class Operation(
    val id: String, val category: Category, val title: String, val desc: String,
    /** Direct USSD code, or null when it runs as a guided *444# session. */
    val directCode: String? = null,
    /** Label of the guided form's primary field, when a form is needed. */
    val formField: String? = null,
    /** In-app destination (TmState.screen) for operations that open a
     *  dedicated screen instead of dialling or a guided sheet. */
    val screen: String? = null,
)

/** Emergency numbers, verbatim from the original app's strings
 *  (Teléfonos útiles). Each row dials via ACTION_DIAL — see Dialer. */
data class EmergencyNumber(val name: String, val number: String, val desc: String)

val EmergencyNumbers: List<EmergencyNumber> = listOf(
    EmergencyNumber("PNR", "106", "Policía Nacional Revolucionaria"),
    EmergencyNumber("Bomberos", "105", "Cuerpo de Bomberos — incendios y rescates"),
    EmergencyNumber("Ambulancia", "104", "Urgencias médicas (SIUM)"),
    EmergencyNumber("Línea Antidrogas", "103", "Línea confidencial contra las drogas"),
    EmergencyNumber("Búsqueda y Salvamento Marítimo", "107", "Emergencias y salvamento en el mar"),
)

val Operations: List<Operation> = listOf(
    // Consultas
    Operation("saldo", Category.CONSULTAS, "Saldo de la tarjeta", "Consulta el saldo de la tarjeta seleccionada.", Ussd.BALANCE),
    Operation("ultimas", Category.CONSULTAS, "Últimas operaciones", "Las operaciones recientes de la tarjeta.", Ussd.LAST_OPS),
    Operation("integrada", Category.CONSULTAS, "Consulta integrada", "Importe de todas las cuentas asociadas.", Ussd.INTEGRATED),
    Operation("limites", Category.CONSULTAS, "Consulta de límites", "Límites de operaciones e importes (ATM/POS).", Ussd.LIMITS),
    Operation("cambio", Category.CONSULTAS, "Tipo de cambio", "Tipo de cambio vigente del banco.", null, "Moneda"),
    Operation("estado_giro", Category.CONSULTAS, "Estado de giro", "Estado de un giro postal.", null, "Código del giro"),
    // Transferencias
    Operation("transf_propia", Category.TRANSFERENCIAS, "A tarjeta propia", "Entre tus tarjetas del mismo banco.", null, "Importe (CUP)"),
    Operation("transf_otra", Category.TRANSFERENCIAS, "A otra tarjeta", "A una tarjeta de otro titular.", null, "Tarjeta destino"),
    Operation("transf_monedero", Category.TRANSFERENCIAS, "A Monedero MiTransfer", "De la tarjeta al monedero.", null, "Importe (CUP)"),
    Operation("caja_extra", Category.TRANSFERENCIAS, "Caja Extra", "Extracción de efectivo en comercios.", null, "Importe (CUP)"),
    Operation("localizar", Category.TRANSFERENCIAS, "Localizar transferencia", "Localiza una transferencia (BANMET).", null, "Nro. de transacción"),
    // Pagos
    Operation("pago_electricidad", Category.PAGOS, "Electricidad", "Factura eléctrica (consultar y pagar).", null, "ID de factura eléctrica"),
    Operation("pago_telefono", Category.PAGOS, "Teléfono fijo", "Factura telefónica / TFA.", null, "Número de teléfono"),
    Operation("pago_agua", Category.PAGOS, "Agua", "Factura de agua.", null, "ID de factura de agua"),
    Operation("pago_gas", Category.PAGOS, "Gas", "Gas licuado / manufacturado.", null, "ID de cliente de gas"),
    Operation("pago_onat", Category.PAGOS, "ONAT", "Tributos, contribuciones y patrimonio.", null, "Código de tributo"),
    Operation("pago_multas", Category.PAGOS, "Multas", "Multas de contravención y de tránsito.", null, "Código de la multa"),
    Operation("pago_combustible", Category.PAGOS, "Combustible", "Cupones de combustible.", null, "Importe (CUP)"),
    Operation("pago_org", Category.PAGOS, "Organizaciones", "Cuotas de organizaciones y PCC.", null, "Organización"),
    // Recargas
    Operation("rec_movil_propio", Category.RECARGAS, "Recarga móvil propia", "Recarga tu propio móvil.", null, "Importe (CUP)"),
    Operation("rec_movil_otro", Category.RECARGAS, "Recarga a otro móvil", "Recarga el móvil de otra persona.", null, "Número de móvil"),
    Operation("rec_nauta", Category.RECARGAS, "Nauta / Nauta Plus", "Recarga una cuenta Nauta.", null, "Cuenta Nauta"),
    Operation("rec_planes", Category.RECARGAS, "Planes Cubacel", "Compra planes con la tarjeta.", Ussd.CUBACEL_PLANS),
    Operation("rec_tfa", Category.RECARGAS, "TFA", "Telefonía fija alternativa.", null, "Número TFA"),
    Operation("rec_jovenclub", Category.RECARGAS, "Joven Club", "Recarga de tiempo de Joven Club.", null, "Usuario"),
    // Cubacel (sin banco)
    Operation("cub_saldo", Category.CUBACEL, "Saldo móvil", "Saldo principal Cubacel.", Ussd.CUBACEL_BALANCE),
    Operation("cub_planes", Category.CUBACEL, "Planes", "Gestión de planes Cubacel.", Ussd.CUBACEL_PLANS),
    Operation("cub_adelanta", Category.CUBACEL, "Adelanta Saldo", "Adelanto de saldo Cubacel.", Ussd.ADELANTA),
    Operation("cub_datos", Category.CUBACEL, "Plan de datos", "Consulta tu plan de datos.", Ussd.CUBACEL_DATA),
    Operation("cub_voz", Category.CUBACEL, "Plan de voz", "Consulta tu plan de voz.", Ussd.CUBACEL_VOICE),
    Operation("cub_sms", Category.CUBACEL, "Plan SMS", "Consulta tu plan de SMS.", Ussd.CUBACEL_SMS),
    Operation("cub_amigos", Category.CUBACEL, "Plan Amigos", "Consulta y gestiona Plan Amigos.", Ussd.PLAN_AMIGOS),
    Operation("cub_bono", Category.CUBACEL, "Bono", "Consulta el bono activo.", Ussd.CUBACEL_BONUS),
    // Monedero MiTransfer
    Operation("mon_saldo", Category.MONEDERO, "Saldo del monedero", "Cuentas CUP y USD del monedero.", null),
    Operation("mon_transf", Category.MONEDERO, "Transferir desde monedero", "Entre monederos o a tarjeta.", null, "Móvil destino"),
    Operation("mon_recarga_cup", Category.MONEDERO, "Recargar cuenta CUP", "Desde la tarjeta al monedero.", null, "Importe (CUP)"),
    Operation("mon_recarga_usd", Category.MONEDERO, "Recargar cuenta USD", "Compra con cuenta USD.", null, "Importe (USD)"),
    Operation("mon_extraction", Category.MONEDERO, "Extracción de efectivo", "Efectivo desde el monedero (CADECA).", null, "Importe (CUP)"),
    // Reservas y trámites
    Operation("miturno", Category.RESERVAS, "MiTurno", "Turnos de banco, gas y CADECA.", null, "Servicio"),
    Operation("ofa", Category.RESERVAS, "OFA Ciudades Patrimoniales", "Reservas y tributos OFA.", null, "Reserva"),
    Operation("giros", Category.RESERVAS, "Giros postales", "Enviar y consultar giros.", null, "Beneficiario"),
    Operation("miboleto", Category.RESERVAS, "MiBoleto", "Compra y consulta de boletos.", null, "Ruta"),
    Operation("pago_linea", Category.RESERVAS, "Pago en Línea", "Pago en línea (incl. Clásica y Empresarial).", null, "Comercio"),
    // Agente — TODO visible y activo: sin código, sin activación, sin puerta.
    Operation("ag_recarga", Category.AGENTE, "Recarga como agente", "Recargas móviles como agente.", null, "Número de móvil"),
    Operation("ag_nauta", Category.AGENTE, "Recarga Nauta como agente", "Recargas Nauta como agente.", null, "Cuenta Nauta"),
    Operation("ag_telecom", Category.AGENTE, "Pago telecom como agente", "Facturas telecom como agente.", null, "Número de teléfono"),
    Operation("ag_saldo", Category.AGENTE, "Saldo del agente", "Saldo de la cuenta de agente.", null),
    Operation("ag_pin", Category.AGENTE, "Cambiar PIN de agente", "PIN de la cuenta de agente.", null, "PIN nuevo"),
    // Gestión
    Operation("ges_tarjetas", Category.GESTION, "Tarjetas", "Tarjetas propias y asociadas (CRUD).", null),
    Operation("ges_clientes", Category.GESTION, "Clientes", "Clientes favoritos desde contactos.", null),
    Operation("ges_facturas", Category.GESTION, "Facturas", "Facturas guardadas (electricidad, agua…).", null),
    Operation("ges_moviles", Category.GESTION, "Móviles favoritos", "Móviles frecuentes desde contactos.", null),
    Operation("ges_fijos", Category.GESTION, "Fijos favoritos", "Teléfonos fijos frecuentes.", null),
    Operation("ges_nauta", Category.GESTION, "Cuentas Nauta", "Cuentas Nauta frecuentes.", null),
    Operation("mi_qr", Category.GESTION, "Mi QR", "Código QR de la tarjeta seleccionada (banco + número) para compartirlo.", null, null, "myqr"),
    // Utilidades
    Operation("telefonos_utiles", Category.UTILIDADES, "Teléfonos útiles", "Emergencias: PNR 106 · Bomberos 105 · Ambulancia 104 · Línea Antidrogas 103 · Salvamento Marítimo 107.", null, null, "emergency"),
)
