/* libtm_c — C11 card utilities for Transfermóvil Expressive.
 * Luhn, BIN->bank, PAN format/mask, amount parsing. Pure, no globals. */
#ifndef TM_C_H
#define TM_C_H
#include <stddef.h>
#ifdef __cplusplus
extern "C" {
#endif
typedef enum { TM_BANK_UNKNOWN = 0, TM_BANK_BPA = 1, TM_BANK_BANDEC = 2, TM_BANK_BANMET = 3 } tm_bank;
/* 1 when the digit string passes the Luhn checksum (13-19 digits). */
int tm_luhn_valid(const char *pan);
/* Bank from BIN prefix: BPA 9238/9204, BANDEC 9224/9243, BANMET 9225/9205
 * (Cuban magnetic-card ranges as used by the *444# platform). */
tm_bank tm_bank_from_bin(const char *pan);
const char *tm_bank_code(tm_bank bank);
/* "9238123456781632" -> "9238 1234 5678 1632" (groups of 4). */
int tm_format_pan(const char *pan, char *buf, size_t buf_size);
/* Keep first 4 + last 4, mask the middle with X: "9238XXXXXXXX1632". */
int tm_mask_pan(const char *pan, char *buf, size_t buf_size);
/* Parse "230.00", "1,500.50", " 89.03 CUP" -> value; returns 1 on success. */
int tm_parse_amount(const char *text, double *out);
#ifdef __cplusplus
}
#endif
#endif
