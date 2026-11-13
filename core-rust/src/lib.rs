//! tm-core: PAGOxMOVIL SMS parser + USSD builders for Transfermóvil Expressive.
pub mod sms;
pub mod ussd;
#[cfg(feature = "jni")]
mod jni_bindings;
pub use sms::{parse_sms, SmsRecord, Bank, SignedAmount, OperationRow};
pub use ussd::*;

fn esc(s: &str) -> String { s.replace('\\', "\\\\").replace('"', "\\\"").replace('\n', "\\n") }

/// Hand-rolled JSON serialisation (no serde; the Kotlin contract).
pub fn record_to_json(r: &SmsRecord) -> String {
    match r {
        SmsRecord::MobileRechargeReceived{amount, currency, from_number, remaining_allowance} =>
            format!("{{\"kind\":\"MobileRechargeReceived\",\"amount\":{},\"currency\":\"{}\",\"fromNumber\":\"{}\",\"remainingAllowance\":{}}}", amount, currency, esc(from_number), remaining_allowance),
        SmsRecord::Authenticated{bank} => format!("{{\"kind\":\"Authenticated\",\"bank\":\"{}\"}}", bank.code()),
        SmsRecord::TransferCompleted{bank, date, beneficiary_pan, source_account, amount, currency, tx_id, remaining_balance} =>
            format!("{{\"kind\":\"TransferCompleted\",\"bank\":\"{}\",\"date\":\"{}\",\"beneficiaryPan\":\"{}\",\"sourceAccount\":\"{}\",\"amount\":{},\"currency\":\"{}\",\"txId\":\"{}\",\"remainingBalance\":{},\"remainingIsCredit\":{}}}", bank.code(), esc(date), esc(beneficiary_pan), esc(source_account), amount, currency, esc(tx_id), remaining_balance.value, remaining_balance.is_credit),
        SmsRecord::BalanceQuery{bank, available_balance} =>
            format!("{{\"kind\":\"BalanceQuery\",\"bank\":\"{}\",\"availableBalance\":{},\"isCredit\":{}}}", bank.code(), available_balance.value, available_balance.is_credit),
        SmsRecord::BillStatus{factura_id, has_debt} =>
            format!("{{\"kind\":\"BillStatus\",\"facturaId\":\"{}\",\"hasDebt\":{}}}", esc(factura_id), has_debt),
        SmsRecord::LastOperations{bank, rows} => {
            let rs: Vec<String> = rows.iter().map(|x| format!("{{\"date\":\"{}\",\"service\":\"{}\",\"op\":\"{}\",\"amount\":{},\"currency\":\"{}\",\"txId\":\"{}\"}}", esc(&x.date), esc(&x.service), esc(&x.op), x.amount, esc(&x.currency), esc(&x.tx_id))).collect();
            format!("{{\"kind\":\"LastOperations\",\"bank\":\"{}\",\"rows\":[{}]}}", bank.code(), rs.join(","))
        }
        SmsRecord::LimitsQuery{bank, card_currency, op_atm, op_pos, amt_atm, amt_pos, amt_total, used_ops_atm, used_ops_pos, used_amt_atm, used_amt_pos} =>
            format!("{{\"kind\":\"LimitsQuery\",\"bank\":\"{}\",\"cardCurrency\":\"{}\",\"opAtm\":{},\"opPos\":{},\"amtAtm\":{},\"amtPos\":{},\"amtTotal\":{},\"usedOpsAtm\":{},\"usedOpsPos\":{},\"usedAmtAtm\":{},\"usedAmtPos\":{}}}", bank.code(), card_currency, op_atm, op_pos, amt_atm, amt_pos, amt_total, used_ops_atm, used_ops_pos, used_amt_atm, used_amt_pos),
        SmsRecord::Unknown{raw} => format!("{{\"kind\":\"Unknown\",\"raw\":\"{}\"}}", esc(raw)),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test] fn json_kind() { let j = record_to_json(&parse_sms("nada")); assert!(j.contains("\"kind\":\"Unknown\""), "{j}"); }
}
