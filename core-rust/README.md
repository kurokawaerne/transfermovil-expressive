# tm-core (Rust)

Pure-Rust, dependency-free core (jni crate only behind `--features jni`):
the PAGOxMOVIL SMS parser (`sms.rs` — typed records for the 9 real
fixture types: MobileRechargeReceived, Authenticated, TransferCompleted,
BalanceQuery, BillStatus, LastOperations, LimitsQuery, Unknown; the
parser never fails, worst case `Unknown`) and USSD builders/validators
(`ussd.rs` — the RE direct-entry map plus the PIN rule). Covered by
`cargo test` (15 tests, fixtures verbatim from real PAGOxMOVIL
messages). Kotlin reaches it through JNI
(`RustCore.nativeParseSms` → hand-rolled JSON); in v1.0 the identical
Kotlin twin (`SmsParser.kt`) runs at runtime, same as ETECSA Toolkit.
