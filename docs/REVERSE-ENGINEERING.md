# Transfermóvil 1.260416 — Reverse Engineering Report

Source APK: `https://www.etecsa.cu/sites/default/files/aplicaciones/Transfermovil%20v.1.260416_0.apk`
Downloaded 2026-11-13. Artefacts kept locally (NOT published) in `~/workspace/transfermovil/`
(`original/`, `reverse/` with `apk_info.json`, `all_strings.txt` ~141k strings, `app_strings.txt`
1,074 UI strings, `jadx/` decompiled output).

## Identity — verified

| Field | Value |
|---|---|
| Package | `cu.etecsa.cubacel.tr.tm` |
| Version | 1.260416 (versionCode 162) |
| Size | 19,957,074 bytes |
| SHA-256 | `b18475eb6911209d21e99d72e6ca888b29236a5b1e470acc70238e05a44f160a` |
| MD5 | `46c7ca33c2ab9596bbfaeadd3b8c6bef` |
| minSdk / targetSdk / compileSdk | 21 / 33 / 34 |
| Main activity | `cu.etecsa.cubacel.tr.tm.prPzLeyxKk` (obfuscated) |
| `android:debuggable` | **true** (in the official release build) |
| Code | Java + Android Views, obfuscated (random class names), 2 dex (11.5 MB + 105 KB), 5,875 classes, JADX 1.5.1: 5,467 files, 13 errors |
| DB | ORMLite (`Operacion`, `AppConfig` models) + SMS-derived local database |
| Native libs | Only ML Kit barcode (`libbarhopper_v3.so`, `libimage_processing_util_jni.so`) — QR scanning; no native business logic |

## How the original works

Transfermóvil is an **offline USSD/SMS front-end** for the `*444#` mobile-banking platform:

1. **USSD sessions.** Operations dial direct-entry codes into the `*444#` platform
   (e.g. `*444*46#`). An **AccessibilityService** (`canRetrieveWindowContent=true`,
   `packageNames="com.android.phone"`, `typeAllMask`) watches the phone app's USSD
   dialogs and automates the session (reads prompts, sends the next step / PIN).
2. **SMS.** A broadcast receiver (`SMS_RECEIVED`) and an inbox query filter on sender
   **`PAGOxMOVIL`**. In the original the SMS is shown as a **raw text alert**
   (`displayAlert(message)`) — no structured parsing. It also exports/imports its
   local records (Clientes, Facturas…) through SMS files.
3. **Online extras (minority).** Deep link `https://transfermovil.app/action?`,
   notifications endpoint `https://mitransfer-notification.tur.cu`, MiBoleto
   (`https://miboleto.mibulevar.com`), OFA Ciudades Patrimoniales web pages,
   self-update from `etecsa.cu` / Apklis.

### Permissions (20)

ACCESS_NETWORK_STATE, CALL_PHONE, CAMERA, INTERNET, MANAGE_EXTERNAL_STORAGE,
READ_CONTACTS, READ_EXTERNAL_STORAGE, READ_MEDIA_IMAGES,
READ_MEDIA_VISUAL_USER_SELECTED, READ_PHONE_STATE, READ_SMS, RECEIVE_BOOT_COMPLETED,
RECEIVE_SMS, REQUEST_INSTALL_PACKAGES, SEND_SMS, VIBRATE, WRITE_EXTERNAL_STORAGE,
WRITE_SMS, telephony feature, own signature permission.

### Direct-entry USSD map (extracted from the operations fragment)

| Code | Operation |
|---|---|
| `*444#` | Platform session start (auth: bank selection → card → PIN) |
| `*444*3#` | Consulta (generic) |
| `*444*13#` | Consulta de saldo (stock) |
| `*444*15#` | Últimas recargas (all) |
| `*444*16#` | Cerrar sesión |
| `*444*46#` | Consultar saldo de la tarjeta |
| `*444*48#` | Últimas operaciones |
| `*444*58#` | Consulta integrada (importe de todas las cuentas) |
| `*444*62#` | Consulta de límites |
| `*444*68*02#` | Eliminar registro de la cuenta |
| `*444*70#` | Session/account operations (multiple, step-differentiated) |
| `*444*91#` | Nauta — consulta opción 1 |
| `*111#` | Servicios Cubacel (selector) |
| `*133#` | Gestión de planes Cubacel |
| `*222#` | Consultar saldo móvil (selector) |
| `*222*264#` | Plan Amigos |
| `*222*266#` | Bono |
| `*222*328#` | Plan de datos |
| `*222*767#` | Plan SMS |
| `*222*869#` | Plan de voz |
| `*234*3#` | Adelanta Saldo |

All other operations (transfers, bill payments, recharges, ONAT, giros, MiTurno,
Caja Extra, fuel coupons…) run as guided steps inside the `*444#` session after
authentication; the app pre-fills what it can and walks the user through prompts.

### Banks (from `assets/agenciasBancarias.json`)

`01` BPA — Banco Popular de Ahorro · `02` BANDEC — Banco de Crédito y Comercio ·
`03` BANMET — Banco Metropolitano.

### The versión-agente gate (removed in the rewrite, per user's order)

Config → "Habilitar versión agente": the user requests an activation code
(delivered via USSD/SMS, read back from the SMS store with
`sms_code_agente(...)`), `check_code_activation(codigo, version, phone)` validates
it **locally**, then every `Operacion` of type ETECSA is set `activo=true` and the
app forces a restart ("close app when active version agent"). A parallel gate
exists for the MiBoleto seller version. **TM Expressive ships all agent and
MiBoleto operations active by default — no code, no gate.**

### The BPA PIN-length bug (user-reported, verified in code)

Bank login forms validate the access PIN inconsistently: several forms require
exactly 4 (`length() != 4`), but at least two accept 4 **or 5**
(`length() != 4 && length() != 5` — classes `…/s9CT6pjRyD.java:619`,
`…/b6FtEHgm1B.java:511`) while the input filters do not stop the extra digit —
so a 5-digit entry passes the field and fails at the bank. TM Expressive enforces
the per-bank PIN length (4) **in the input itself**: the field physically cannot
hold a 5th digit.

### Bundled offline catalogues (`assets/*.json`, extracted)

Bank branches: BPA 231, BANDEC 230, BANMET 70, CADECA 61 · MiTurno: provinces (16),
services (108 branch services), gas branches (44 + 141 balita + 449 La Habana) ·
ONAT/OFA: tributos 37, reservas 21, DPA provinces/municipalities · Cubacel plan
offer groups (7) · traffic-law articles (95, for multas) · stamp denominations (26).
These are ETECSA/bank factual data and are re-hosted as static JSON in the rewrite.

### Feature inventory (from the 1,074 UI strings + menus)

- **Auth/session:** register (Registrarse), authenticate per bank, change access
  PIN, change Multibanca PIN, close session, delete registration, default card.
- **Cards (Tarjetas propias):** add/edit/delete, set default, reprint request.
- **Consultas:** card balance, integrated balance, last operations, limits,
  exchange rate (tipo de cambio), payment state, giro state, bono state.
- **Transferencias:** to own/other cards, to Monedero, Caja Extra, locate transfer (BANMET).
- **Pagos:** electricity, fixed phone/telecom bill (consult + pay + rectify),
  water, gas, ONAT (tributos, contribuciones, patrimonio, PCC/organizaciones),
  multas (contravención + tránsito), fuel coupons, sindicato payments.
- **Recargas:** mobile balance (own/other, bono), Nauta + Nauta Plus, TFA,
  Joven Club, Cubacel plans with USD account (Bolsa), FWA fixed wireless.
- **Monedero MiTransfer:** register, balance, transfer, recharge CUP/USD accounts,
  buy Nauta Plus / data plans with USD, cash extraction, CADECA office recharge.
- **Servicios:** MiTurno reservations (bank, gas, CADECA), OFA Ciudades
  Patrimoniales, giros postales, MiBoleto (buy/consult), Pago en Línea
  (incl. Clásica + Empresarial), factura management (add/consult/pay/delete),
  gestión de clientes / móviles / fijos / Nauta accounts (favourites with contacts).
- **Agente (gated in original):** mobile/Nauta recharges as agent, telecom payment
  as agent, agent balance, agent PIN change.
- **Cubacel (non-bank):** saldo `*222#`, planes `*133#`, Adelanta `*234*3#`, bono.
- **Utilities:** emergency numbers (PNR, bomberos, ambulancia, antidrogas,
  salvamento marítimo), QR scan/generate (payment QR), notifications inbox,
  settings (themes in original are minimal), backup/export via files and SMS.

### Branding

`res/drawable/logo_solo_transfermovil.png` (7,181 bytes, extracted): the
double-arrow mark — green arrow up-right, blue arrow down-left — with the
TRANSFERMÓVIL wordmark in a heavy italic sans. **No font file is embedded** in
the APK; the rewrite redraws the mark as SVG and sets the wordmark in
Google Sans Flex (heavy italic), per the user's instruction.

## Clean-room boundary

TM Expressive is implemented from this spec (menus, codes, catalogues, SMS
formats) — no decompiled Transfermóvil code is copied or published, and the
`*444#` platform itself (ETECSA/banks) is only *used* through the same public
USSD/SMS interfaces the original uses.
