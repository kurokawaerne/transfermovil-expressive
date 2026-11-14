package dev.kurokawa.transfermovil.expressive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.*
import com.composables.icons.materialsymbols.outlinedfilled.*
import dev.kurokawa.transfermovil.expressive.data.TmDb
import dev.kurokawa.transfermovil.expressive.model.*
import dev.kurokawa.transfermovil.expressive.platform.Dialer
import dev.kurokawa.transfermovil.expressive.theme.TransfermovilTheme
import kotlinx.coroutines.launch

/** App state: bank/card selection drives every operation form. */
class TmState(val db: TmDb) {
    var screen by mutableStateOf("home")
    var bankIndex by mutableIntStateOf(0)
    var cards by mutableStateOf(db.cards())
    var pendingOp by mutableStateOf<Operation?>(null)
    val banks: List<Bank> = Bank.entries
    val bank: Bank get() = banks[bankIndex.coerceIn(0, banks.lastIndex)]
    fun cardsFor(b: Bank): List<BankCard> = cards.filter { it.bank == b }
    /** The card screens act on (Mi QR, guided sheets): the bank's
     *  default card when one is marked, otherwise its first card. */
    fun selectedCard(b: Bank = bank): BankCard? =
        cardsFor(b).firstOrNull { it.isDefault } ?: cardsFor(b).firstOrNull()
    fun refreshCards() { cards = db.cards() }
    fun back() { screen = "home" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TmApp(startScreen: String = "home") {
    val ctx = LocalContext.current
    val db = remember { TmDb(ctx) }
    val state = remember { TmState(db).apply { screen = startScreen } }
    var dark by remember { mutableStateOf(false) }
    TransfermovilTheme(darkMode = dark) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (state.screen) {
                        "home" -> HomeScreen(state)
                        "auth" -> AuthScreen(state)
                        "services" -> ServicesScreen(state)
                        "sms" -> SmsInboxScreen(state)
                        "manage" -> ManageScreen(state)
                        "emergency" -> EmergencyNumbersScreen(state)
                        "myqr" -> MyQrScreen(state)
                        "settings" -> SettingsScreen(state, dark) { dark = it }
                        else -> HomeScreen(state)
                    }
                }
                // ShortNavigationBar LOOSE-CONSTRAINTS CURE (convicted on
                // UDYAT/LSEx): width lives on this wrapper Box; the bar
                // itself only gets widthIn(max) — never fillMaxWidth(),
                // which inflates every item to full bar width.
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ShortNavigationBar(Modifier.widthIn(max = 420.dp).padding(bottom = 12.dp, top = 4.dp)) {
                        ShortNavigationBarItem(
                            selected = state.screen == "home",
                            onClick = { state.screen = "home" },
                            icon = { Icon(MaterialSymbols.Outlined.Home, "Inicio") },
                            label = { Text("Inicio") })
                        ShortNavigationBarItem(
                            selected = state.screen == "services",
                            onClick = { state.screen = "services" },
                            icon = { Icon(MaterialSymbols.Outlined.Apps, "Servicios") },
                            label = { Text("Servicios") })
                        ShortNavigationBarItem(
                            selected = state.screen == "sms",
                            onClick = { state.screen = "sms" },
                            icon = { Icon(MaterialSymbols.Outlined.Sms, "PAGOxMOVIL") },
                            label = { Text("SMS") })
                        ShortNavigationBarItem(
                            selected = state.screen == "settings",
                            onClick = { state.screen = "settings" },
                            icon = { Icon(MaterialSymbols.Outlined.Settings, "Ajustes") },
                            label = { Text("Ajustes") })
                    }
                }
            }
        }
        // Guided step sheet for session operations.
        state.pendingOp?.let { op -> GuidedStepSheet(state, op) { state.pendingOp = null } }
    }
}

/** Home: nested-pager card carousel. Horizontal swipe changes BANK,
 *  vertical swipe changes the CARD of the current bank. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: TmState) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val bankPager = rememberPagerState(initialPage = state.bankIndex) { state.banks.size }
    LaunchedEffect(bankPager.currentPage) { state.bankIndex = bankPager.currentPage }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TRANSFER", fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall)
                Text("MÓVIL", fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                Text("Expressive", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            HorizontalPager(state = bankPager, modifier = Modifier.fillMaxWidth().height(230.dp)) { page ->
                val bank = state.banks[page]
                val bankCards = state.cardsFor(bank)
                if (bankCards.isEmpty()) {
                    BankCardPlaceholder(bank) { state.screen = "manage" }
                } else {
                    val cardPager = rememberPagerState { bankCards.size }
                    VerticalPager(state = cardPager, modifier = Modifier.fillMaxSize()) { cp ->
                        BankCardView(bank, bankCards[cp], cp + 1, bankCards.size)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                state.banks.forEachIndexed { i, b ->
                    Box(Modifier.padding(horizontal = 3.dp).size(if (i == bankPager.currentPage) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (i == bankPager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        .clickable { scope.launch { bankPager.animateScrollToPage(i) } })
                    @Suppress("UNUSED_EXPRESSION") b
                }
            }
            Text("Desliza ↔ para cambiar de banco · ↕ para cambiar de tarjeta",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { state.screen = "auth" }, modifier = Modifier.weight(1f)) { Text("Autenticarse") }
                OutlinedButton(onClick = { Dialer.dialUssd(ctx, Ussd.BALANCE) }, modifier = Modifier.weight(1f)) { Text("Saldo *444*46#") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { state.screen = "myqr" }, modifier = Modifier.weight(1f)) {
                    Icon(MaterialSymbols.Outlined.Qr_code, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Mi QR")
                }
                OutlinedButton(onClick = { state.screen = "emergency" }, modifier = Modifier.weight(1f)) {
                    Icon(MaterialSymbols.Outlined.Call, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Teléfonos útiles")
                }
            }
        }
        item { Text("Accesos rápidos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("Consultas", "Saldo, últimas operaciones, límites", "consultas"),
                    Triple("Transferencias", "A tarjetas, Monedero y Caja Extra", "transferencias"),
                    Triple("Pagos", "Electricidad, teléfono, agua, gas, ONAT, multas…", "pagos"),
                    Triple("Recargas", "Móvil, Nauta, planes, TFA, Joven Club", "recargas"),
                ).forEach { (t, d, _) ->
                    Card(Modifier.fillMaxWidth().clickable { state.screen = "services" }, shape = RoundedCornerShape(24.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(t, fontWeight = FontWeight.SemiBold)
                                Text(d, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(MaterialSymbols.Outlined.Chevron_right, null)
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { Dialer.dialUssd(ctx, Ussd.CLOSE_SESSION) }, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar sesión (*444*16#)")
            }
        }
    }
}

@Composable
fun BankCardView(bank: Bank, card: BankCard, index: Int, total: Int) {
    val gradient = Brush.linearGradient(listOf(Color(bank.brandArgb), Color(bank.brandArgb).copy(alpha = 0.65f)))
    Card(Modifier.fillMaxSize().padding(horizontal = 4.dp), shape = RoundedCornerShape(28.dp)) {
        Box(Modifier.fillMaxSize().background(gradient).padding(20.dp)) {
            Column(Modifier.fillMaxSize()) {
                Row {
                    Text(bank.code, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    Text("$index / $total ↕", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                }
                Text(bank.fullName, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Text(card.masked.chunked(4).joinToString(" "), color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
                Row {
                    Text(card.alias.ifEmpty { card.currency }, color = Color.White.copy(alpha = 0.85f))
                    Spacer(Modifier.weight(1f))
                    if (card.isDefault) Text("Predeterminada", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    if (card.associated) Text("Asociada", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun BankCardPlaceholder(bank: Bank, onAdd: () -> Unit) {
    Card(Modifier.fillMaxSize().padding(horizontal = 4.dp), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(bank.fullName, fontWeight = FontWeight.SemiBold)
            Text("Sin tarjetas de ${bank.code} todavía.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAdd) { Text("Añadir tarjeta") }
        }
    }
}

/** Authentication: per-bank PIN with pinLength=4 enforced IN THE INPUT
 *  (filter + maxLength) — the BPA 5-digit bug fix by construction. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(state: TmState) {
    val ctx = LocalContext.current
    var bank by remember { mutableStateOf(state.bank) }
    var pin by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { state.back() }) { Icon(MaterialSymbols.Outlined.Arrow_back, "Atrás") }
                Text("Autenticarse", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
        }
        item { Text("Banco", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Bank.entries.filter { it != Bank.MONEDERO }.forEach { b ->
                    FilterChip(selected = bank == b, onClick = { bank = b; pin = "" }, label = { Text(b.code) })
                }
            }
        }
        item {
            OutlinedTextField(
                value = pin,
                onValueChange = { input ->
                    // THE FIX: digits only AND capped at the bank's
                    // pinLength — a 5th digit cannot be typed, pasted
                    // or autofilled, for any bank.
                    pin = input.filter { it.isDigit() }.take(bank.pinLength)
                },
                label = { Text("PIN de Telebanca (${bank.pinLength} dígitos)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                supportingText = { Text("${pin.length}/${bank.pinLength} dígitos") },
                modifier = Modifier.fillMaxWidth())
        }
        item {
            Button(
                onClick = { state.db.addHistory("Autenticación", bank.code); Dialer.dialUssd(ctx, Ussd.SESSION) },
                enabled = pin.length == bank.pinLength,
                modifier = Modifier.fillMaxWidth()) { Text("Autenticarse en ${bank.code} (*444#)") }
        }
        item {
            Text("La sesión se abre en la plataforma *444#: elige el banco, la tarjeta y escribe este mismo PIN cuando la plataforma lo pida. La app nunca guarda el PIN en claro.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedButton(onClick = { Dialer.dialUssd(ctx, Ussd.SESSION) }, modifier = Modifier.fillMaxWidth()) { Text("Cambiar PIN de acceso (sesión *444#)") }
            OutlinedButton(onClick = { Dialer.dialUssd(ctx, Ussd.SESSION) }, modifier = Modifier.fillMaxWidth()) { Text("Cambiar PIN Multibanca (sesión *444#)") }
            OutlinedButton(onClick = { Dialer.dialUssd(ctx, Ussd.DELETE_REGISTRATION) }, modifier = Modifier.fillMaxWidth()) { Text("Eliminar registro (*444*68*02#)") }
        }
    }
}

/** Guided step sheet: shows the exact *444# step sequence with the
 *  values to type pre-computed, then dials the session. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidedStepSheet(state: TmState, op: Operation, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var field by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(op.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(op.desc, style = MaterialTheme.typography.bodyMedium)
            Text("Banco: ${state.bank.code} · Tarjeta: ${state.cardsFor(state.bank).firstOrNull()?.masked ?: "—"}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (op.formField != null) {
                OutlinedTextField(value = field, onValueChange = { field = it },
                    label = { Text(op.formField) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            Text("Pasos en la plataforma *444#:", fontWeight = FontWeight.SemiBold)
            listOf(
                "1. Autentícate en ${state.bank.code} con tu PIN de ${state.bank.pinLength} dígitos.",
                "2. Elige «${op.title}» en el menú de la plataforma.",
                if (op.formField != null) "3. Escribe: ${field.ifEmpty { "«${op.formField}»" }} cuando lo pida." else "3. Confirma la operación.",
                "4. El resultado llega por SMS de PAGOxMOVIL y aparecerá analizado en la pestaña SMS.",
            ).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Button(onClick = {
                state.db.addHistory(op.title, field)
                val code = op.directCode ?: Ussd.SESSION
                onDismiss(); Dialer.dialUssd(ctx, code)
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (op.directCode != null) "Marcar ${op.directCode}" else "Abrir sesión *444#")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
