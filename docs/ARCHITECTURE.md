# Transfermóvil Expressive — Architecture

Public repo: `kurokawaerne/transfermovil-expressive` (source + releases, one repo,
same as ETECSA Toolkit). Package `dev.kurokawa.transfermovil.expressive` —
installs beside the official Transfermóvil. Android only (the platform is
USSD/SMS telephony; no desktop shell). Spanish UI (the original is Spanish-only;
strings externalised for a future EN pass).

## Stack

- **Kotlin + Jetpack Compose**, Material 3 Expressive — official components
  first (ShortNavigationBar with the loose-constraints cure, HorizontalPager,
  ModalBottomSheet, SearchBar, SegmentedButtons → connected ToggleButtons,
  LoadingIndicator, wavy progress only for the verifying state), Material
  Symbols in the DEFAULT style (no Rounded), Google Sans Flex (static
  instances), dynamic colour.
- **Rust core (`tm-core`)** — the critical text machinery: PAGOxMOVIL SMS
  parser, USSD string builders/validators. JNI bindings; a Kotlin twin of the
  parser runs until the JNI link is wired (same pattern as ETECSA Toolkit v1.0).
- **C core (`libtm_c`)** — PAN/card utilities: Luhn check, BIN→bank detection
  (BPA 9238…, BANDEC/BANMET ranges), card formatting/masking, amount parsing.
  CMake/NDK, with a host-side test binary.
- **Local DB** — Android SQLite via SQLiteOpenHelper (no Room, per the ETK
  decision): cards, favourites (clientes/móviles/fijos/nauta), facturas,
  parsed SMS records, operation history.
- **CI is the build compiler of record** (local Gradle is proxy-blocked on
  Luna's VM — same as LSEx/ETK). GitHub Actions: split APKs
  (arm64-v8a, armeabi-v7a, x86_64, universal) + SHA256SUMS.

## Execution model (how operations run)

Same public interfaces as the original:

- **USSD:** direct-entry codes from the RE map are dialled with
  `ACTION_CALL` (`tel:<code Uri.encode("#")>`); session operations open
  `*444#` after an in-app guided form (amount, beneficiary, factura…) that
  shows the exact step sequence the platform will ask, with the values to type
  pre-computed. An optional AccessibilityService (user-enabled, watches
  `com.android.phone`) assists by reading prompts — shipped as an opt-in,
  never required.
- **Authentication state:** the app tracks bank + selected card; authenticating
  is a `*444#` session action; the app never stores the bank PIN in plain
  text (Android Keystore-wrapped, and only if the user opts in to "remember").
- **SMS:** `RECEIVE_SMS`/`READ_SMS` on sender `PAGOxMOVIL` → Rust parser →
  structured records presented as MD3E cards; raw text always one tap away.

## The card carousel (user's headline interaction)

Home shows the selected bank's card as a big MD3E card.

- **Swipe left/right** → previous/next **bank** (BPA → BANDEC → BANMET →
  Monedero), with the card colour/branding cross-fading.
- **Swipe up/down** → previous/next **card of the current bank** (the user's
  tarjetas propias + associated cards for that bank).
- Implemented as nested pagers (outer HorizontalPager for banks, inner
  VerticalPager for cards) with expressive motion; dots/indicators for both
  axes; the same selection drives every operation form's "from card" default.

## PAGOxMOVIL SMS parser spec (Rust, `tm-core::sms`)

Input: raw SMS body from sender `PAGOxMOVIL`. Output: a typed record.
Fixtures = the user's 9 real examples (2026-11-13); all must parse in tests.

| # | Example (abridged) | Type | Extracted fields |
|---|---|---|---|
| 1 | "Usted ha recibido una recarga de saldo movil por un monto de 360 CUP del numero 5356535695. Monto restante a recargar para el periodo: 0 CUP…" | `MobileRechargeReceived` | amount 360 CUP, fromNumber 5356535695, remainingAllowance 0 CUP |
| 2 | "Usted se ha autenticado en la plataforma de pagos moviles, en el Banco Metropolitano…" | `Authenticated` | bank = BANMET |
| 3 | "Banco Metropolitano: La Transferencia fue completada. Fecha: 10/11/2026 Beneficiario: 9238XXXXXXXX1632 Ordenante: CUP Monto: 230.00 CUP Nro. Transaccion: AY601CU4XU987 Saldo restante: CR 174.15 CUP" | `TransferCompleted` | bank, date, beneficiaryPan (masked), sourceAccount "CUP", amount, txId, remainingBalance (CR/DB + value) |
| 4 | "Usted se ha autenticado… Banco Popular de Ahorro…" | `Authenticated` | bank = BPA |
| 5 | "Banco Popular de Ahorro: La consulta de saldo fue completada. Saldo Disponible: CR 89.03 CUP" | `BalanceQuery` | bank, availableBalance CR 89.03 CUP |
| 6 | "Consulta de servicio completada. La Factura: 3401019104154 no tiene deudas" | `BillStatus` | facturaId, hasDebt=false |
| 7 | "Banco Popular de Ahorro Ultimas operaciones. Fecha;Servicio;Operacion;Monto;Moneda;NoTransaccion 24/09/2026;OTR;DB;4000.00;CUP;AY600OT3YP997 \| …" | `LastOperations` | bank + rows[] {date, service (may be empty), op CR/DB, amount, currency, txId} — rows separated by `\|`, fields by `;` |
| 8 | "Banco Popular de Ahorro: Consulta de Limites: Moneda Tarjeta: CUP Cantidad operaciones: ATM:40 POS:40 Importe: ATM:50000 POS:50000 Total:100000 Consumido: Operaciones ATM:0 POS:0 Importe ATM:0 POS:0." | `LimitsQuery` | bank, cardCurrency, opLimits {ATM, POS}, amountLimits {ATM, POS, total}, consumed {ops ATM/POS, amounts ATM/POS} |
| 9 | (any other PAGOxMOVIL text) | `Unknown` | raw body preserved, shown unparsed with a "report" affordance |

Parsing rules: amounts are `#,##0.00` with `.` decimal; balances may carry a
`CR`/`DB` prefix; bank names map "Banco Metropolitano"→BANMET,
"Banco Popular de Ahorro"→BPA, "Banco de Crédito y Comercio"→BANDEC;
dates are `dd/MM/yyyy`; the parser never fails — worst case `Unknown`.
Presentation: one MD3E card per record (icon + type title, key amounts as
large display numbers, CR green / DB red chips, txId in mono, expandable raw
text), filterable by bank/type, newest first — replacing the original's raw
alert dialog.

## The PIN rule (bug fix by construction)

Per-bank credential lengths are data (`Bank.pinLength = 4` for BPA/BANDEC/
BANMET telebanking PIN). Login and change-PIN fields take `maxLength` from
that data and filter input to it — a 5th digit cannot be typed, pasted, or
autofilled. Validation then only checks completeness.

## Services taxonomy (categories, MD3E)

Consultas · Transferencias · Pagos (Electricidad, Teléfono, Agua, Gas, ONAT,
Multas, Combustible, Organizaciones) · Recargas (Móvil, Nauta, Planes, TFA,
Joven Club) · Monedero MiTransfer · Reservas y Trámites (MiTurno, OFA, Giros,
MiBoleto, Pago en Línea) · Cubacel (Saldo, Planes, Adelanta, Bono) · Agente
(always unlocked) · Gestión (Tarjetas, Clientes, Facturas, Móviles/Fijos/Nauta).

## Platform features

Animated splash (single, dynamic-coloured, system splash + Compose overlay
on first frame only — no double splash), predictive back, bubbles
(Android 17-ready `Notification.BubbleMetadata` for parsed-SMS results and
session reminders), contacts with photos for favourites/beneficiaries,
global search over services + favourites, gradient adaptive icon built from
the redrawn double-arrow mark + monochrome themed icon, wordmark as SVG
redraw of `logo_solo_transfermovil.png` (no embeddable font exists in the
original) with Google Sans Flex heavy-italic text.

## Milestones (shipped as ONE v1.0.0 batch, per the user's batching rule)

M0 scaffold + CI → M1 auth/cards/carousel + consultas → M2 transfers + pagos +
facturas → M3 recargas + Cubacel + Monedero → M4 SMS parser + inbox UI →
M5 MiTurno/OFA/MiBoleto/Pago en Línea + Agente → M6 polish (splash, bubbles,
search, icon) → v1.0.0.
