# Changelog

## Sin publicar

- Teléfonos útiles (Utilidades): PNR 106, Bomberos 105, Ambulancia
  104, Línea Antidrogas 103, Búsqueda y Salvamento Marítimo 107 —
  cada número abre el marcador (ACTION_DIAL). Registrado en el
  catálogo de Servicios y en la búsqueda global.
- Mi QR: código QR de la tarjeta seleccionada (carga = código de
  banco + PAN, ZXing core), con acceso desde Inicio y Gestión.

## 1.0.0 — 2026-11-13

Initial release. Clean-room Material 3 Expressive rewrite of
Transfermóvil 1.260416 (see `docs/REVERSE-ENGINEERING.md`).

- Card carousel: nested pagers — swipe ↔ bank, swipe ↕ card.
- Authentication with the BPA PIN bug fixed by construction
  (per-bank `pinLength = 4` enforced in the input: filter + maxLength).
- Consultas (*444*46/48/58/62#), transferencias, pagos (electricidad,
  teléfono, agua, gas, ONAT, multas, combustible, organizaciones),
  recargas, Cubacel (*222#/*133#/*234*3#/…), Monedero MiTransfer,
  Reservas y Trámites, Agente (all operations active, no gate),
  Gestión with contacts-backed favourites.
- PAGOxMOVIL SMS parsed by the Rust `tm-core` parser (Kotlin twin at
  runtime in v1.0) into MD3E cards with raw text one tap away.
- Rust + C native cores with host tests; SQLite local DB; gradient
  adaptive + themed icon; single dynamic-coloured system splash;
  predictive back; bubble-ready SMS notifications.
