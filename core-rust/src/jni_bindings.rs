//! Thin JNI bindings (feature `jni`), mirroring ETK's pattern: panic-free
//! wrappers over the pure functions. Kotlin class:
//! dev.kurokawa.transfermovil.expressive.core.RustCore
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;
use crate::{parse_sms, record_to_json};

fn read(env: &mut JNIEnv, s: &JString) -> Option<String> {
    env.get_string(s).ok().map(|j| j.to_string_lossy().into_owned())
}

#[no_mangle]
pub extern "system" fn Java_dev_kurokawa_transfermovil_expressive_core_RustCore_nativeParseSms<'local>(
    mut env: JNIEnv<'local>, _class: JClass<'local>, body: JString<'local>,
) -> jstring {
    let text = read(&mut env, &body).unwrap_or_default();
    let json = record_to_json(&parse_sms(&text));
    env.new_string(json).map(|s| s.into_raw()).unwrap_or(std::ptr::null_mut())
}
