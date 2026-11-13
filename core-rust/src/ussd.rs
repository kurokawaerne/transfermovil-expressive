//! USSD builders/validators from the RE map (REVERSE-ENGINEERING.md).

pub fn session_start() -> &'static str { "*444#" }
pub fn balance() -> &'static str { "*444*46#" }
pub fn last_operations() -> &'static str { "*444*48#" }
pub fn integrated_balance() -> &'static str { "*444*58#" }
pub fn limits() -> &'static str { "*444*62#" }
pub fn close_session() -> &'static str { "*444*16#" }
pub fn cubacel_balance() -> &'static str { "*222#" }
pub fn cubacel_plans() -> &'static str { "*133#" }
pub fn adelanta_saldo() -> &'static str { "*234*3#" }
pub fn cubacel_data() -> &'static str { "*222*328#" }
pub fn cubacel_voice() -> &'static str { "*222*869#" }
pub fn cubacel_sms() -> &'static str { "*222*767#" }
pub fn plan_amigos() -> &'static str { "*222*264#" }
pub fn cubacel_bonus() -> &'static str { "*222*266#" }

/// A code is valid when it starts with * (or is a tel: number), uses only
/// digits/*/# and ends with # for USSD.
pub fn is_valid_ussd(code: &str) -> bool {
    if code.is_empty() || code.len() > 32 { return false; }
    if !code.chars().all(|c| c.is_ascii_digit() || c == '*' || c == '#') { return false; }
    code.starts_with('*') && code.ends_with('#')
}

/// PIN rule: exactly `pin_length` digits (4 for all banks). The 5-digit
/// BPA bug is impossible when callers also cap input at pin_length.
pub fn is_valid_pin(pin: &str, pin_length: usize) -> bool {
    pin.len() == pin_length && pin.chars().all(|c| c.is_ascii_digit())
}

pub fn build_code(segments: &[&str]) -> String {
    let mut out = String::from("*");
    out.push_str(&segments.join("*"));
    out.push('#');
    out
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test] fn known_codes_valid() { for c in [balance(), last_operations(), integrated_balance(), limits(), cubacel_balance(), adelanta_saldo()] { assert!(is_valid_ussd(c), "{c}"); } }
    #[test] fn invalid_codes_rejected() { assert!(!is_valid_ussd("")); assert!(!is_valid_ussd("444")); assert!(!is_valid_ussd("*444")); assert!(!is_valid_ussd("*44a#")); }
    #[test] fn pin_rule() { assert!(is_valid_pin("1234", 4)); assert!(!is_valid_pin("12345", 4)); assert!(!is_valid_pin("12a4", 4)); assert!(!is_valid_pin("123", 4)); }
    #[test] fn build() { assert_eq!(build_code(&["444", "46"]), "*444*46#"); }
}
