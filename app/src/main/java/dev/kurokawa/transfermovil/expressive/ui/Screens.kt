package dev.kurokawa.transfermovil.expressive.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.materialsymbols.MaterialSymbols
import dev.kurokawa.transfermovil.expressive.core.QrCode
import dev.kurokawa.transfermovil.expressive.core.SmsRecord
import dev.kurokawa.transfermovil.expressive.data.Contacts
import dev.kurokawa.transfermovil.expressive.data.Factura
import dev.kurokawa.transfermovil.expressive.data.Favourite
import dev.kurokawa.transfermovil.expressive.model.*
import dev.kurokawa.transfermovil.expressive.platform.Dialer
import dev.kurokawa.transfermovil.expressive.sms.PagoInbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Servicios: categories from the taxonomy + global search over
 *  services and favourites. Direct codes dial immediately; session
 *  operations open the guided step sheet. Agente has NO gate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(state: TmState) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<Category?>(null) }
    val favourites = remember(state.screen) { state.db.favourites() }
    val filtered = Operations.filter { op ->
        (category == null || op.category == category) &&
            (query.isBlank() || op.title.contains(query, true) || op.desc.contains(query, true))
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Servicios", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) }
        item {
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = query, onQueryChange = { query = it },
                        onSearch = {}, expanded = false, onExpandedChange = {},
                        placeholder = { Text("Buscar servicios y favoritos…") },
                        leadingIcon = { Icon(MaterialSymbols.Outlined.Search, null) })
                },
                expanded = false, onExpandedChange = {},
                modifier = Modifier.fillMaxWidth()) {}
        }
        if (query.isNotBlank() && favourites.any { it.name.contains(query, true) || it.value.contains(query, true) }) {
            item { Text("Favoritos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(favourites.filter { it.name.contains(query, true) || it.value.contains(query, true) }) { f ->
                FavRow(f) { state.db.deleteFavourite(f.id) }
            }
        }
        item {
            // Horizontally scrollable: with ten categories the second
            // row no longer fits the screen width in a fixed Row.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                FilterChip(selected = category == null, onClick = { category = null }, label = { Text("Todos") })
                Category.entries.take(3).forEach { c ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.title) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                Category.entries.drop(3).forEach { c ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.title) })
                }
            }
        }
        Category.entries.forEach { c ->
            val ops = filtered.filter { it.category == c }
            if (ops.isNotEmpty()) {
                item {
                    Text(c.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp))
                    if (c == Category.AGENTE) {
                        Text("Todas las operaciones de agente están activas — sin código de activación.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                items(ops) { op ->
                    Card(Modifier.fillMaxWidth().clickable {
                        val target = op.screen
                        when {
                            target != null -> {
                                state.db.addHistory(op.title, "pantalla")
                                state.screen = target
                            }
                            op.directCode != null && op.formField == null -> {
                                state.db.addHistory(op.title, op.directCode)
                                Dialer.dialUssd(ctx, op.directCode)
                            }
                            else -> {
                                state.db.addHistory(op.title, "sesión *444#")
                                state.pendingOp = op
                            }
                        }
                    }, shape = RoundedCornerShape(24.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(op.title, fontWeight = FontWeight.SemiBold)
                                Text(op.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                op.directCode?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
                            }
                            Icon(MaterialSymbols.Outlined.Chevron_right, null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavRow(f: Favourite, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Text(f.name.take(1).uppercase(), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(f.name, fontWeight = FontWeight.SemiBold)
                Text("${f.kind} · ${f.value}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                f.photoUri?.let { Text("Foto de contacto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
            }
            IconButton(onClick = onDelete) { Icon(MaterialSymbols.Outlined.Delete, "Eliminar") }
        }
    }
}

/** SMS PAGOxMOVIL inbox: parsed records as MD3E cards, raw expandable,
 *  filterable by bank/type, newest first — replaces the raw alert. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsInboxScreen(state: TmState) {
    val ctx = LocalContext.current
    var inbox by remember { mutableStateOf(state.db.smsInbox()) }
    var kindFilter by remember { mutableStateOf<String?>(null) }
    var bankFilter by remember { mutableStateOf<Bank?>(null) }
    LaunchedEffect(Unit) { PagoInbox.scan(ctx); inbox = state.db.smsInbox() }
    val filtered = inbox.filter { s ->
        (kindFilter == null || s.record.kind == kindFilter) &&
            (bankFilter == null || recordBank(s.record) == bankFilter)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("SMS de PAGOxMOVIL", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "Todos", "TransferCompleted" to "Transferencias", "BalanceQuery" to "Saldos", "LastOperations" to "Operaciones").forEach { (k, label) ->
                    FilterChip(selected = kindFilter == k, onClick = { kindFilter = k }, label = { Text(label) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = bankFilter == null, onClick = { bankFilter = null }, label = { Text("Todos los bancos") })
                Bank.entries.filter { it != Bank.MONEDERO }.forEach { b ->
                    FilterChip(selected = bankFilter == b, onClick = { bankFilter = b }, label = { Text(b.code) })
                }
            }
        }
        if (filtered.isEmpty()) {
            item { Text("Sin mensajes analizados todavía. Los SMS de PAGOxMOVIL aparecen aquí automáticamente, ya interpretados.",
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(filtered) { sms -> SmsCard(sms.record, sms.raw) }
    }
}

fun recordBank(r: SmsRecord): Bank? = when (r) {
    is SmsRecord.Authenticated -> r.bank
    is SmsRecord.TransferCompleted -> r.bank
    is SmsRecord.BalanceQuery -> r.bank
    is SmsRecord.LastOperations -> r.bank
    is SmsRecord.LimitsQuery -> r.bank
    else -> null
}

@Composable
fun SmsCard(record: SmsRecord, raw: String) {
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(MaterialSymbols.Outlined.Sms, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(titleFor(record), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                recordBank(record)?.let { AssistChip(onClick = {}, label = { Text(it.code) }) }
            }
            when (record) {
                is SmsRecord.MobileRechargeReceived -> {
                    BigAmount(record.amount, record.currency)
                    Text("De: ${record.fromNumber} · Restante del periodo: ${fmt(record.remainingAllowance)} ${record.currency}")
                }
                is SmsRecord.Authenticated -> Text("Sesión iniciada en ${record.bank?.fullName ?: "el banco"}.")
                is SmsRecord.TransferCompleted -> {
                    BigAmount(record.amount, record.currency)
                    Text("Para: ${record.beneficiaryPan} · ${record.date}")
                    Text("Transacción: ${record.txId}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    BalanceChip(record.remainingBalance.isCredit, record.remainingBalance.value, record.remainingBalance.currency, "Saldo restante")
                }
                is SmsRecord.BalanceQuery -> BalanceChip(record.availableBalance.isCredit, record.availableBalance.value, record.availableBalance.currency, "Saldo disponible")
                is SmsRecord.BillStatus -> Text("Factura ${record.facturaId}: ${if (record.hasDebt) "con deudas" else "sin deudas"}")
                is SmsRecord.LastOperations -> {
                    record.rows.take(5).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(row.date, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(row.service.ifEmpty { "—" }, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text("${row.op} ${fmt(row.amount)} ${row.currency}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (row.op == "CR") Color(0xFF2CA800) else MaterialTheme.colorScheme.error)
                        }
                    }
                    if (record.rows.size > 5) Text("… y ${record.rows.size - 5} más", style = MaterialTheme.typography.bodySmall)
                }
                is SmsRecord.LimitsQuery -> {
                    Text("Moneda: ${record.cardCurrency} · Operaciones ATM ${record.opAtm} / POS ${record.opPos}")
                    Text("Importes: ATM ${fmt(record.amtAtm)} · POS ${fmt(record.amtPos)} · Total ${fmt(record.amtTotal)}")
                }
                is SmsRecord.Unknown -> Text("Mensaje sin formato conocido.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Ocultar texto original" else "Ver texto original") }
            if (expanded) Text(raw, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun titleFor(r: SmsRecord): String = when (r) {
    is SmsRecord.MobileRechargeReceived -> "Recarga recibida"
    is SmsRecord.Authenticated -> "Autenticación"
    is SmsRecord.TransferCompleted -> "Transferencia completada"
    is SmsRecord.BalanceQuery -> "Consulta de saldo"
    is SmsRecord.BillStatus -> "Estado de factura"
    is SmsRecord.LastOperations -> "Últimas operaciones"
    is SmsRecord.LimitsQuery -> "Consulta de límites"
    is SmsRecord.Unknown -> "PAGOxMOVIL"
}

fun fmt(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)

@Composable
fun BigAmount(amount: Double, currency: String) {
    Text("${fmt(amount)} $currency", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
fun BalanceChip(isCredit: Boolean, value: Double, currency: String, label: String) {
    AssistChip(onClick = {}, label = {
        Text("$label: ${if (isCredit) "CR" else "DB"} ${fmt(value)} $currency")
    }, colors = AssistChipDefaults.assistChipColors(
        containerColor = if (isCredit) Color(0xFFB7F39A) else MaterialTheme.colorScheme.errorContainer))
}

/** Gestión: tarjetas CRUD + favourites from contacts + facturas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(state: TmState) {
    val ctx = LocalContext.current
    var pan by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf(Bank.BPA) }
    var showContacts by remember { mutableStateOf(false) }
    var facturaCode by remember { mutableStateOf("") }
    val contacts = remember(showContacts) { if (showContacts) Contacts.read(ctx) else emptyList() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { state.back() }) { Icon(MaterialSymbols.Outlined.Arrow_back, "Atrás") }
                Text("Gestión", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            Card(Modifier.fillMaxWidth().clickable { state.screen = "myqr" }, shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(MaterialSymbols.Outlined.Qr_code, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Mi QR", fontWeight = FontWeight.SemiBold)
                        Text("Código QR de la tarjeta seleccionada (${state.bank.code}) para compartirla.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(MaterialSymbols.Outlined.Chevron_right, null)
                }
            }
        }
        item { Text("Tarjetas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        items(state.cards) { card ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${card.bank.code} · ${card.masked}", fontWeight = FontWeight.SemiBold)
                        Text(card.alias.ifEmpty { card.currency }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { state.db.deleteCard(card.id); state.refreshCards() }) { Icon(MaterialSymbols.Outlined.Delete, "Eliminar") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Bank.entries.filter { it != Bank.MONEDERO }.forEach { b ->
                    FilterChip(selected = bank == b, onClick = { bank = b }, label = { Text(b.code) })
                }
            }
            OutlinedTextField(value = pan, onValueChange = { pan = it.filter { c -> c.isDigit() }.take(16) },
                label = { Text("Número de tarjeta (16 dígitos)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = alias, onValueChange = { alias = it },
                label = { Text("Alias (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                state.db.addCard(BankCard(bank = bank, pan = pan, alias = alias))
                pan = ""; alias = ""; state.refreshCards()
            }, enabled = pan.length == 16, modifier = Modifier.fillMaxWidth()) { Text("Añadir tarjeta") }
        }
        item { Text("Favoritos desde contactos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        item {
            OutlinedButton(onClick = { showContacts = !showContacts }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showContacts) "Ocultar contactos" else "Elegir de mis contactos (con foto)")
            }
        }
        items(contacts.take(20)) { c ->
            Card(Modifier.fillMaxWidth().clickable {
                state.db.addFavourite(Favourite(kind = "movil", name = c.name, value = c.number, photoUri = c.photoUri))
            }, shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Text(c.name.take(1).uppercase(), fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(c.name, fontWeight = FontWeight.SemiBold); Text(c.number, style = MaterialTheme.typography.bodySmall) }
                    Icon(MaterialSymbols.Outlined.Add, "Añadir favorito")
                }
            }
        }
        items(state.db.favourites()) { f -> FavRow(f) { state.db.deleteFavourite(f.id) } }
        item { Text("Facturas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        items(state.db.facturas()) { f ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("${f.kind} · ${f.code}", fontWeight = FontWeight.SemiBold); Text(f.alias, style = MaterialTheme.typography.bodySmall) }
                    IconButton(onClick = { state.db.deleteFactura(f.id) }) { Icon(MaterialSymbols.Outlined.Delete, "Eliminar") }
                }
            }
        }
        item {
            OutlinedTextField(value = facturaCode, onValueChange = { facturaCode = it },
                label = { Text("Código de factura (ej. eléctrica)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { state.db.addFactura(Factura(kind = "electricidad", code = facturaCode)); facturaCode = "" },
                enabled = facturaCode.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Guardar factura") }
        }
        item { Text("Historial", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        items(state.db.history()) { (t, d) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(MaterialSymbols.Outlined.History, null)
                Spacer(Modifier.width(8.dp))
                Column { Text(t, fontWeight = FontWeight.SemiBold); Text(d, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

/** Settings: Play-Store-style grouped panel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: TmState, dark: Boolean, onDark: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { state.back() }) { Icon(MaterialSymbols.Outlined.Arrow_back, "Atrás") }
                Text("Ajustes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Transfermóvil Expressive", fontWeight = FontWeight.Bold)
                    Text("Versión 1.0.0 · Reescritura MD3E de código abierto (MIT). Sin versión-agente: todo activo.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Text("Apariencia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Tema oscuro", modifier = Modifier.weight(1f))
                Switch(checked = dark, onCheckedChange = onDark)
            }
            Text("Color dinámico (Material You) sigue al sistema; el icono es adaptativo y temático.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { Text("Sesión y plataforma", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        item {
            OutlinedButton(onClick = { state.screen = "auth" }, modifier = Modifier.fillMaxWidth()) { Text("Autenticarse") }
            OutlinedButton(onClick = { state.screen = "manage" }, modifier = Modifier.fillMaxWidth()) { Text("Gestión (tarjetas, favoritos, facturas)") }
            OutlinedButton(onClick = { Dialer.dialUssd(ctx, Ussd.CLOSE_SESSION) }, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión (*444*16#)") }
        }
        item { Text("Acerca de", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        item {
            Text("Núcleos nativos: Rust (analizador SMS PAGOxMOVIL + constructores USSD) y C (Luhn, BIN→banco, importes). Base local SQLite. Las operaciones usan las mismas interfaces públicas *444#/SMS del original.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Per-number icon for Teléfonos útiles (Material Symbols, default
 *  style, outlined — same policy as the rest of the app). */
fun emergencyIcon(e: EmergencyNumber): ImageVector = when (e.number) {
    "106" -> MaterialSymbols.Outlined.Local_police
    "105" -> MaterialSymbols.Outlined.Local_fire_department
    "104" -> MaterialSymbols.Outlined.Ambulance
    "103" -> MaterialSymbols.Outlined.Pill_off
    "107" -> MaterialSymbols.Outlined.Directions_boat
    else -> MaterialSymbols.Outlined.Call
}

/** Teléfonos útiles: the original app's emergency numbers. Tapping a
 *  row opens the number in the system dialer (ACTION_DIAL tel:) so
 *  the user confirms the call — no CALL_PHONE permission involved. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyNumbersScreen(state: TmState) {
    val ctx = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { state.back() }) { Icon(MaterialSymbols.Outlined.Arrow_back, "Atrás") }
                Text("Teléfonos útiles", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            Text("Emergencias en Cuba. Toca un número para abrirlo en el marcador y confirmar la llamada.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(EmergencyNumbers) { e ->
            Card(Modifier.fillMaxWidth().clickable {
                state.db.addHistory("Teléfonos útiles", "${e.name} ${e.number}")
                Dialer.dialNumber(ctx, e.number)
            }, shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(emergencyIcon(e), null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.name, fontWeight = FontWeight.SemiBold)
                        Text(e.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(e.number, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Icon(MaterialSymbols.Outlined.Call, "Llamar a ${e.number}")
                }
            }
        }
    }
}

/** Mi QR: QR of the currently selected card (payload = bank code +
 *  PAN, see QrCode.payload). The bitmap is generated off the UI
 *  thread and rendered on a white surface so it stays scannable in
 *  dark mode too. Bank chips follow the Home selection; card chips
 *  switch between the bank's cards. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQrScreen(state: TmState) {
    var pickedCardId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(state.bankIndex) { pickedCardId = null }
    val bankCards = state.cardsFor(state.bank)
    val card = bankCards.firstOrNull { it.id == pickedCardId } ?: state.selectedCard()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { state.back() }) { Icon(MaterialSymbols.Outlined.Arrow_back, "Atrás") }
                Text("Mi QR", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                state.banks.forEachIndexed { i, b ->
                    FilterChip(selected = state.bankIndex == i, onClick = { state.bankIndex = i }, label = { Text(b.code) })
                }
            }
        }
        if (card == null) {
            item {
                Text("Sin tarjetas de ${state.bank.code} todavía. Añade una en Gestión para generar su código QR.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Button(onClick = { state.screen = "manage" }, modifier = Modifier.fillMaxWidth()) { Text("Ir a Gestión") }
            }
        } else {
            if (bankCards.size > 1) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        bankCards.forEach { c ->
                            FilterChip(selected = c.id == card.id, onClick = { pickedCardId = c.id },
                                label = { Text("···· " + c.masked.takeLast(4)) })
                        }
                    }
                }
            }
            item {
                val payload = remember(card.bank.code, card.pan) { QrCode.payload(card.bank.code, card.pan) }
                val qrBitmap by produceState<Bitmap?>(initialValue = null, payload) {
                    value = withContext(Dispatchers.Default) { QrCode.bitmap(payload) }
                }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
                        Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            val bmp = qrBitmap
                            if (bmp != null) {
                                Image(bitmap = bmp.asImageBitmap(), contentDescription = "Código QR de la tarjeta",
                                    modifier = Modifier.size(260.dp))
                            } else {
                                Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                    Text("${card.bank.code} · ${card.masked.chunked(4).joinToString(" ")}",
                        fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    card.alias.takeIf { it.isNotEmpty() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(payload, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Muestra este código para que te transfieran a esta tarjeta: contiene el banco y el número de la tarjeta.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
