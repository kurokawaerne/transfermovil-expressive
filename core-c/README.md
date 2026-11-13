# libtm_c (C11)

Card utilities where a wrong answer costs money: Luhn checksum,
BIN→bank detection (BPA 9238/9204, BANDEC 9224/9243, BANMET 9225/9205),
PAN formatting (`9238 1234 …`) and PAGOxMOVIL-style masking
(`9238XXXXXXXX1632`), and tolerant amount parsing (`1,500.50`,
`89.03 CUP`). Pure functions, no globals. Build + test:
`cmake -S . -B build && cmake --build build && ./build/tm_c_test`
prints PASS. The Android build links the same sources via CMake/NDK.
