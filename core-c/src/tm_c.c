#include "tm_c.h"
#include <ctype.h>
#include <string.h>

static int digits_only(const char *s, size_t *len) {
    if (!s) return 0;
    size_t n = 0;
    for (const char *p = s; *p; p++) { if (!isdigit((unsigned char)*p)) return 0; n++; }
    *len = n;
    return n > 0;
}

int tm_luhn_valid(const char *pan) {
    size_t len;
    if (!digits_only(pan, &len) || len < 13 || len > 19) return 0;
    int sum = 0, dbl = 0;
    for (size_t i = len; i-- > 0;) {
        int d = pan[i] - '0';
        if (dbl) { d *= 2; if (d > 9) d -= 9; }
        sum += d; dbl = !dbl;
    }
    return sum % 10 == 0;
}

tm_bank tm_bank_from_bin(const char *pan) {
    if (!pan || strlen(pan) < 4) return TM_BANK_UNKNOWN;
    if (strncmp(pan, "9238", 4) == 0 || strncmp(pan, "9204", 4) == 0) return TM_BANK_BPA;
    if (strncmp(pan, "9224", 4) == 0 || strncmp(pan, "9243", 4) == 0) return TM_BANK_BANDEC;
    if (strncmp(pan, "9225", 4) == 0 || strncmp(pan, "9205", 4) == 0) return TM_BANK_BANMET;
    return TM_BANK_UNKNOWN;
}

const char *tm_bank_code(tm_bank bank) {
    switch (bank) {
        case TM_BANK_BPA: return "BPA";
        case TM_BANK_BANDEC: return "BANDEC";
        case TM_BANK_BANMET: return "BANMET";
        default: return "UNKNOWN";
    }
}

static int write_grouped(const char *pan, int mask, char *buf, size_t buf_size) {
    size_t len;
    if (!digits_only(pan, &len) || !buf || buf_size == 0) return -1;
    size_t need = len + (len - 1) / 4 + 1;
    if (buf_size < need) return -1;
    size_t o = 0;
    for (size_t i = 0; i < len; i++) {
        if (i > 0 && i % 4 == 0) buf[o++] = ' ';
        buf[o++] = (mask && i >= 4 && i + 4 < len) ? 'X' : pan[i];
    }
    buf[o] = '\0';
    return (int)o;
}
int tm_format_pan(const char *pan, char *buf, size_t n) { return write_grouped(pan, 0, buf, n); }
int tm_mask_pan(const char *pan, char *buf, size_t n) {
    /* Mask without spaces, matching PAGOxMOVIL style: 9238XXXXXXXX1632 */
    size_t len;
    if (!digits_only(pan, &len) || !buf || n < len + 1) return -1;
    for (size_t i = 0; i < len; i++) buf[i] = (i >= 4 && i + 4 < len) ? 'X' : pan[i];
    buf[len] = '\0';
    return (int)len;
}

int tm_parse_amount(const char *text, double *out) {
    if (!text || !out) return 0;
    const char *p = text;
    while (*p == ' ' || *p == '\t') p++;
    double whole = 0, frac = 0, scale = 1;
    int seen = 0, dot = 0;
    for (; *p; p++) {
        if (isdigit((unsigned char)*p)) {
            seen = 1;
            if (!dot) whole = whole * 10 + (*p - '0');
            else { scale *= 10; frac += (*p - '0') / scale; }
        } else if (*p == ',') { continue; }
        else if (*p == '.' && !dot) { dot = 1; }
        else break;
    }
    if (!seen) return 0;
    *out = whole + frac;
    return 1;
}
