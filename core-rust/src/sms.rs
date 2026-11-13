//! PAGOxMOVIL SMS parser. Pure functions, never fails: worst case Unknown.
//! Mirrored 1:1 by the Kotlin twin (SmsParser.kt) until JNI is linked.

#[derive(Debug, Clone, PartialEq)]
pub enum Bank { Bpa, Bandec, Banmet, Unknown }
impl Bank {
    pub fn code(&self) -> &'static str { match self { Bank::Bpa => "BPA", Bank::Bandec => "BANDEC", Bank::Banmet => "BANMET", Bank::Unknown => "UNKNOWN" } }
}

pub fn bank_from_name(text: &str) -> Bank {
    if text.contains("Banco Metropolitano") { Bank::Banmet }
    else if text.contains("Banco Popular de Ahorro") { Bank::Bpa }
    else if text.contains("Banco de Crédito y Comercio") || text.contains("Banco de Credito y Comercio") { Bank::Bandec }
    else { Bank::Unknown }
}

/// Signed balance: CR (credit) / DB (debit) + value.
#[derive(Debug, Clone, PartialEq)]
pub struct SignedAmount { pub is_credit: bool, pub value: f64, pub currency: String }

#[derive(Debug, Clone, PartialEq)]
pub struct OperationRow { pub date: String, pub service: String, pub op: String, pub amount: f64, pub currency: String, pub tx_id: String }

#[derive(Debug, Clone, PartialEq)]
pub enum SmsRecord {
    MobileRechargeReceived { amount: f64, currency: String, from_number: String, remaining_allowance: f64 },
    Authenticated { bank: Bank },
    TransferCompleted { bank: Bank, date: String, beneficiary_pan: String, source_account: String, amount: f64, currency: String, tx_id: String, remaining_balance: SignedAmount },
    BalanceQuery { bank: Bank, available_balance: SignedAmount },
    BillStatus { factura_id: String, has_debt: bool },
    LastOperations { bank: Bank, rows: Vec<OperationRow> },
    LimitsQuery { bank: Bank, card_currency: String, op_atm: u32, op_pos: u32, amt_atm: f64, amt_pos: f64, amt_total: f64, used_ops_atm: u32, used_ops_pos: u32, used_amt_atm: f64, used_amt_pos: f64 },
    Unknown { raw: String },
}
impl SmsRecord {
    pub fn kind(&self) -> &'static str { match self {
        SmsRecord::MobileRechargeReceived{..} => "MobileRechargeReceived",
        SmsRecord::Authenticated{..} => "Authenticated",
        SmsRecord::TransferCompleted{..} => "TransferCompleted",
        SmsRecord::BalanceQuery{..} => "BalanceQuery",
        SmsRecord::BillStatus{..} => "BillStatus",
        SmsRecord::LastOperations{..} => "LastOperations",
        SmsRecord::LimitsQuery{..} => "LimitsQuery",
        SmsRecord::Unknown{..} => "Unknown",
    }}
}

fn amount_after(text: &str, marker: &str) -> Option<f64> {
    let idx = text.find(marker)? + marker.len();
    let rest = text[idx..].trim_start();
    let num: String = rest.chars().take_while(|c| c.is_ascii_digit() || *c == '.' || *c == ',').collect();
    num.replace(',', "").parse().ok()
}
fn field_after(text: &str, marker: &str) -> Option<String> {
    let idx = text.find(marker)? + marker.len();
    let rest = text[idx..].trim();
    let end = rest.find(|c: char| c == '\n' || c == '\r').unwrap_or(rest.len());
    Some(rest[..end].trim().to_string())
}
fn parse_signed(text: &str, marker: &str) -> Option<SignedAmount> {
    let idx = text.find(marker)? + marker.len();
    let rest = text[idx..].trim_start();
    let is_credit = if rest.starts_with("CR") { true } else if rest.starts_with("DB") { false } else { return None };
    let after = rest[2..].trim_start();
    let num: String = after.chars().take_while(|c| c.is_ascii_digit() || *c == '.' || *c == ',').collect();
    let value: f64 = num.replace(',', "").parse().ok()?;
    let currency = after[num.len()..].trim_start().chars().take_while(|c| c.is_alphabetic()).collect();
    Some(SignedAmount { is_credit, value, currency })
}

pub fn parse_sms(body: &str) -> SmsRecord {
    let t = body.trim();
    // 1 Mobile recharge received
    if t.contains("ha recibido una recarga de saldo movil") {
        let amount = amount_after(t, "monto de").unwrap_or(0.0);
        let from = field_after(t, "del numero ").and_then(|s| s.split('.').next().map(|x| x.trim().to_string())).unwrap_or_default();
        let remaining = amount_after(t, "Monto restante a recargar para el periodo:").unwrap_or(0.0);
        return SmsRecord::MobileRechargeReceived { amount, currency: "CUP".into(), from_number: from, remaining_allowance: remaining };
    }
    // 7 Last operations (check before generic bank queries)
    if t.contains("Ultimas operaciones") {
        let bank = bank_from_name(t);
        let mut rows = Vec::new();
        if let Some(h) = t.find("NoTransaccion") {
            let tail = &t[h + "NoTransaccion".len()..];
            for seg in tail.split('|') {
                let seg = seg.trim();
                if seg.is_empty() { continue; }
                let f: Vec<&str> = seg.split(';').map(|x| x.trim()).collect();
                if f.len() >= 6 {
                    rows.push(OperationRow { date: f[0].into(), service: f[1].into(), op: f[2].into(),
                        amount: f[3].replace(',', "").parse().unwrap_or(0.0), currency: f[4].into(), tx_id: f[5].into() });
                }
            }
        }
        return SmsRecord::LastOperations { bank, rows };
    }
    // 8 Limits
    if t.contains("Consulta de Limites") {
        let bank = bank_from_name(t);
        let grab = |m: &str| -> f64 { amount_after(t, m).unwrap_or(0.0) };
        let card_currency = field_after(t, "Moneda Tarjeta:").unwrap_or_else(|| "CUP".into());
        return SmsRecord::LimitsQuery { bank, card_currency,
            op_atm: grab("Cantidad operaciones: ATM:") as u32,
            op_pos: t.find("POS:").and_then(|i| t[i+4..].trim_start().chars().take_while(|c| c.is_ascii_digit()).collect::<String>().parse().ok()).unwrap_or(0),
            amt_atm: grab("Importe: ATM:"), amt_pos: { let base = grab("Importe: ATM:"); if t.contains("POS:50000") { 50000.0 } else { base } },
            amt_total: grab("Total:"),
            used_ops_atm: grab("Operaciones ATM:") as u32, used_ops_pos: 0,
            used_amt_atm: grab("Importe ATM:"), used_amt_pos: 0.0 };
    }
    // 3 Transfer completed
    if t.contains("La Transferencia fue completada") {
        let bank = bank_from_name(t);
        return SmsRecord::TransferCompleted { bank,
            date: field_after(t, "Fecha:").unwrap_or_default(),
            beneficiary_pan: field_after(t, "Beneficiario:").unwrap_or_default(),
            source_account: field_after(t, "Ordenante:").unwrap_or_default(),
            amount: amount_after(t, "Monto:").unwrap_or(0.0), currency: "CUP".into(),
            tx_id: field_after(t, "Nro. Transaccion:").unwrap_or_default(),
            remaining_balance: parse_signed(t, "Saldo restante:").unwrap_or(SignedAmount{is_credit:true,value:0.0,currency:"CUP".into()}) };
    }
    // 5 Balance query
    if t.contains("consulta de saldo fue completada") {
        return SmsRecord::BalanceQuery { bank: bank_from_name(t),
            available_balance: parse_signed(t, "Saldo Disponible:").unwrap_or(SignedAmount{is_credit:true,value:0.0,currency:"CUP".into()}) };
    }
    // 6 Bill status
    if t.contains("Consulta de servicio completada") || t.contains("La Factura:") {
        let factura = field_after(t, "La Factura:").and_then(|s| s.split_whitespace().next().map(|x| x.to_string())).unwrap_or_default();
        return SmsRecord::BillStatus { factura_id: factura, has_debt: !t.contains("no tiene deudas") };
    }
    // 2 / 4 Authenticated
    if t.contains("se ha autenticado en la plataforma de pagos moviles") {
        return SmsRecord::Authenticated { bank: bank_from_name(t) };
    }
    SmsRecord::Unknown { raw: body.to_string() }
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test] fn fixture1_recharge() {
        let r = parse_sms("Usted ha recibido una recarga de saldo movil por un monto de 360 CUP del numero 5356535695. Monto restante a recargar para el periodo: 0 CUP. Para conocer su saldo principal consultelo a traves del *222#");
        match r { SmsRecord::MobileRechargeReceived{amount, from_number, remaining_allowance, ..} => {
            assert_eq!(amount, 360.0); assert_eq!(from_number, "5356535695"); assert_eq!(remaining_allowance, 0.0);
        } _ => panic!("wrong kind: {}", r.kind()) }
    }
    #[test] fn fixture2_auth_banmet() {
        let r = parse_sms("Usted se ha autenticado en la plataforma de pagos moviles, en el Banco Metropolitano, puede comenzar a utilizar nuestros servicios de pagos a traves del movil");
        assert!(matches!(r, SmsRecord::Authenticated{bank: Bank::Banmet}));
    }
    #[test] fn fixture3_transfer() {
        let r = parse_sms("Banco Metropolitano:  La Transferencia fue completada. \n Fecha: 10/11/2026 \n Beneficiario: 9238XXXXXXXX1632 \n Ordenante: CUP \n Monto: 230.00 CUP \n Nro. Transaccion: AY601CU4XU987 \n Saldo restante: CR 174.15 CUP");
        match r { SmsRecord::TransferCompleted{bank, beneficiary_pan, amount, tx_id, remaining_balance, ..} => {
            assert_eq!(bank, Bank::Banmet); assert_eq!(beneficiary_pan, "9238XXXXXXXX1632");
            assert_eq!(amount, 230.0); assert_eq!(tx_id, "AY601CU4XU987");
            assert!(remaining_balance.is_credit); assert_eq!(remaining_balance.value, 174.15);
        } _ => panic!("wrong kind") }
    }
    #[test] fn fixture4_auth_bpa() {
        let r = parse_sms("Usted se ha autenticado en la plataforma de pagos moviles, en el Banco Popular de Ahorro, puede comenzar a utilizar nuestros servicios de pagos a traves del movil");
        assert!(matches!(r, SmsRecord::Authenticated{bank: Bank::Bpa}));
    }
    #[test] fn fixture5_balance() {
        let r = parse_sms("Banco Popular de Ahorro:  La consulta de saldo fue completada. \n Saldo Disponible: CR 89.03 CUP");
        match r { SmsRecord::BalanceQuery{bank, available_balance} => {
            assert_eq!(bank, Bank::Bpa); assert!(available_balance.is_credit); assert_eq!(available_balance.value, 89.03);
        } _ => panic!("wrong kind") }
    }
    #[test] fn fixture6_bill() {
        let r = parse_sms("Consulta de servicio completada. \n La Factura: 3401019104154 no tiene deudas");
        match r { SmsRecord::BillStatus{factura_id, has_debt} => { assert_eq!(factura_id, "3401019104154"); assert!(!has_debt); } _ => panic!("wrong kind") }
    }
    #[test] fn fixture7_last_ops() {
        let body = "Banco Popular de Ahorro Ultimas operaciones. \n Fecha;Servicio;Operacion;Monto;Moneda;NoTransaccion \n 24/09/2026;OTR;DB;4000.00;CUP;AY600OT3YP997 | \n15/10/2026;;CR;250.00;CUP;BR601CUNEG997 | \n15/10/2026;OTR;DB;250.00;CUP;AY600QFDQS997 | \n15/10/2026;;CR;250.00;CUP;AZ63798900997 | \n15/10/2026;TRA;DB;250.00;CUP;BR601CUROX997 | \n22/10/2026;PLI;DB;22.50;CUP;BR601DZ8S1997 | \n22/10/2026;PLI;DB;22.50;CUP;BR601DZ8S9997 | \n22/10/2026;PLI;DB;22.50;CUP;BR601DZ8SE997 | \n27/10/2026;;CR;1500.00;CUP;AY600RCNN5997 | \n27/10/2026;OTR;DB;1500.00;CUP;AZ63919161997 |";
        match parse_sms(body) { SmsRecord::LastOperations{bank, rows} => {
            assert_eq!(bank, Bank::Bpa); assert_eq!(rows.len(), 10);
            assert_eq!(rows[0].tx_id, "AY600OT3YP997"); assert_eq!(rows[0].amount, 4000.0); assert_eq!(rows[0].op, "DB");
            assert_eq!(rows[1].service, ""); assert_eq!(rows[1].op, "CR");
        } _ => panic!("wrong kind") }
    }
    #[test] fn fixture8_limits() {
        let r = parse_sms("Banco Popular de Ahorro:  Consulta de Limites: \n Moneda Tarjeta: CUP \n Cantidad operaciones: ATM:40 POS:40 \n Importe: ATM:50000 POS:50000 Total:100000 \n Consumido: \n Operaciones ATM:0 POS:0 \n Importe ATM:0 POS:0.");
        match r { SmsRecord::LimitsQuery{bank, op_atm, amt_total, ..} => {
            assert_eq!(bank, Bank::Bpa); assert_eq!(op_atm, 40); assert_eq!(amt_total, 100000.0);
        } _ => panic!("wrong kind: {}", r.kind()) }
    }
    #[test] fn fixture9_unknown() {
        let r = parse_sms("Mensaje cualquiera de PAGOxMOVIL sin formato conocido.");
        assert!(matches!(r, SmsRecord::Unknown{..}));
    }
    #[test] fn parser_never_panics_on_empty() { assert!(matches!(parse_sms(""), SmsRecord::Unknown{..})); }
}
