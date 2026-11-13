/* Host test for libtm_c: prints PASS when every assertion holds. */
#include <math.h>
#include <stdio.h>
#include <string.h>
#include "tm_c.h"
static int failures = 0;
static void expect_int(const char *w, int got, int want) { if (got != want) { printf("FAIL %s: got %d want %d\n", w, got, want); failures++; } }
static void expect_str(const char *w, const char *got, const char *want) { if (strcmp(got, want) != 0) { printf("FAIL %s: got \"%s\" want \"%s\"\n", w, got, want); failures++; } }
static void expect_dbl(const char *w, double got, double want) { if (fabs(got - want) > 1e-9) { printf("FAIL %s: got %.4f want %.4f\n", w, got, want); failures++; } }
int main(void) {
    /* 4111111111111111 is the canonical Luhn-valid test PAN. */
    expect_int("luhn valid", tm_luhn_valid("4111111111111111"), 1);
    expect_int("luhn invalid", tm_luhn_valid("4111111111111112"), 0);
    expect_int("luhn short", tm_luhn_valid("4111"), 0);
    expect_int("luhn letters", tm_luhn_valid("4111a11111111111"), 0);
    expect_int("bin BPA", tm_bank_from_bin("9238000011112222"), TM_BANK_BPA);
    expect_int("bin BANDEC", tm_bank_from_bin("9224000011112222"), TM_BANK_BANDEC);
    expect_int("bin BANMET", tm_bank_from_bin("9225000011112222"), TM_BANK_BANMET);
    expect_int("bin unknown", tm_bank_from_bin("4111111111111111"), TM_BANK_UNKNOWN);
    expect_str("code BPA", tm_bank_code(TM_BANK_BPA), "BPA");
    char buf[64];
    tm_format_pan("9238123456781632", buf, sizeof(buf));
    expect_str("format", buf, "9238 1234 5678 1632");
    tm_mask_pan("9238123456781632", buf, sizeof(buf));
    expect_str("mask", buf, "9238XXXXXXXX1632");
    double v = 0;
    expect_int("amount ok", tm_parse_amount("230.00", &v), 1); expect_dbl("amount 230", v, 230.0);
    expect_int("amount comma", tm_parse_amount("1,500.50", &v), 1); expect_dbl("amount 1500.50", v, 1500.50);
    expect_int("amount suffix", tm_parse_amount(" 89.03 CUP", &v), 1); expect_dbl("amount 89.03", v, 89.03);
    expect_int("amount bad", tm_parse_amount("CUP", &v), 0);
    if (failures) { printf("%d FAILURE(S)\n", failures); return 1; }
    printf("PASS\n");
    return 0;
}
