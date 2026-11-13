# Transfermóvil Expressive

**Transfermóvil, rehecho en Material 3 Expressive: todas las funciones, sin puerta de «versión agente», con los SMS de PAGOxMOVIL por fin analizados y presentados como tarjetas.**

**Transfermóvil rebuilt in Material 3 Expressive: every feature, no agent-version gate, and PAGOxMOVIL SMS finally parsed into proper cards instead of a raw alert.**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Platform](https://img.shields.io/badge/platform-Android_8.0%2B-green)

Clean-room rewrite of Transfermóvil 1.260416 (`cu.etecsa.cubacel.tr.tm`) from the reverse-engineering spec in [`docs/REVERSE-ENGINEERING.md`](docs/REVERSE-ENGINEERING.md) — no decompiled code is used or published. The app drives the same public interfaces as the original: USSD sessions on the `*444#` platform (`ACTION_CALL` with encoded `#`, guided step sheets for session operations) and SMS from `PAGOxMOVIL`.

- **Package:** `dev.kurokawa.transfermovil.expressive` — installs beside the official Transfermóvil
- **UI:** Spanish (the original is Spanish-only), Google Sans Flex, Material Symbols in the default style
- **Stack:** Kotlin + Jetpack Compose (Material 3 Expressive) · Rust core (`tm-core`) · C core (`libtm_c`) · SQLite via `SQLiteOpenHelper`

---

## ✨ Features

### 🏠 Card carousel (headline interaction)
- Home is a nested-pager carousel: **swipe ↔** changes bank (BPA → BANDEC → BANMET → Monedero MiTransfer), **swipe ↕** changes the card of the current bank, with bank branding cross-fading and indicators on both axes. The same selection drives every operation form's "from card" default.

### 🔐 Authentication — with the PIN bug fixed by construction
- The original accepts 5 digits on some BPA login forms while the bank PIN is 4 — the entry passes the field and fails at the bank. Here `Bank.pinLength = 4` is data and every PIN field takes its filter + `maxLength` from it: **a 5th digit cannot be typed, pasted or autofilled.**
- Register, authenticate per bank, change access PIN, change Multibanca PIN, close session (`*444*16#`), delete registration (`*444*68*02#`). The PIN is never stored in plain text.

### 📊 Consultas · 💸 Transferencias · 🧾 Pagos · 🔋 Recargas
- Consultas: saldo (`*444*46#`), últimas operaciones (`*444*48#`), integrada (`*444*58#`), límites (`*444*62#`), tipo de cambio, estado de giro.
- Transferencias: a tarjetas propias/otras, a Monedero, Caja Extra, localizar transferencia (BANMET).
- Pagos in guided forms: electricidad, teléfono, agua, gas, ONAT (catalogue of 37 tributos in assets), multas, combustible, organizaciones.
- Recargas: móvil propia/otro, Nauta y Nauta Plus, planes, TFA, Joven Club.
- Cubacel sin banco: saldo `*222#`, planes `*133#`, Adelanta `*234*3#`, datos/voz/SMS/bono/Plan Amigos (`*222*328/869/767/266/264#`).
- Monedero MiTransfer: saldo, transferencias, recargas CUP/USD, extracción CADECA.
- Reservas y Trámites: MiTurno, OFA Ciudades Patrimoniales, giros postales, MiBoleto, Pago en Línea.
- **Agente: every operation visible and active — the versión-agente activation gate is removed entirely.**

### 💬 SMS PAGOxMOVIL, parsed
- A `BroadcastReceiver` + inbox scan filter on sender `PAGOxMOVIL`; the Rust parser (with an identical Kotlin twin running in v1.0) turns each message into a typed record — recarga recibida, autenticación, transferencia completada, saldo, factura, últimas operaciones (tabla completa), límites — shown as MD3E cards with large amounts, CR-green / DB-red chips, monospace transaction ids, filters by bank and type, and the raw text always one tap away.

### 🗂️ Gestión y datos locales
- Tarjetas (CRUD, predeterminada, asociadas), clientes / móviles / fijos / cuentas Nauta as favourites **from your contacts, with photo/initial avatars**, facturas guardadas, historial — all in on-device SQLite. Factual catalogues from the original (banks, branches, tributos, DPA, planes) ship as JSON in `assets/`.

### 🎨 Design & platform
- Material 3 Expressive with official components first (the floating `ShortNavigationBar` uses the loose-constraints cure: width on a wrapper, never `fillMaxWidth` on the bar), services broken into categories with global search over services + favourites.
- New icon: the double-arrow mark redrawn as vector, in Google's gradient style, as an adaptive icon with a monochrome themed variant; wordmark set in Google Sans Flex heavy italic (no font was embedded in the original APK).
- Single dynamic-coloured system splash (no double splash), predictive back (`enableOnBackInvokedCallback`), and bubble-ready `Notification.BubbleMetadata` notifications for parsed SMS.

## 🧪 Native cores

```sh
cargo test --manifest-path core-rust/Cargo.toml   # 15 tests, 9 real SMS fixtures
cmake -S core-c -B core-c/build && cmake --build core-c/build && ./core-c/build/tm_c_test  # PASS
```

CI (`.github/workflows/`) runs both native test suites and builds split APKs (arm64-v8a, armeabi-v7a, x86_64, universal) + `SHA256SUMS.txt` per release.

## 📄 License

MIT — see [LICENSE](LICENSE). Transfermóvil and PAGOxMOVIL are ETECSA/bank trademarks; this is an independent clean-room client for the same public USSD/SMS platform.
