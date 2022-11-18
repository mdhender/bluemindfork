export const PKCS7_MIMES = ["application/pkcs7-mime", "application/x-pkcs7-mime"];
export const MULTIPART_SIGNED_MIME = "multipart/signed";

export const SMIME_INTERNAL_API_URL = "/service-worker-internal/smime";
export enum PKIStatus {
    EMPTY = 0,
    CERTIFICATE_OK = 1,
    PRIVATE_KEY_OK = 2,
    OK = PRIVATE_KEY_OK | CERTIFICATE_OK
}
export enum PKIEntry {
    PRIVATE_KEY = "privateKey",
    CERTIFICATE = "certificate"
}
export const ENCRYPTED_HEADER_NAME = "X-BM-Encrypted";
export const SIGNED_HEADER_NAME = "X-BM-Signed";
export const CRYPTO_HEADERS = {
    DECRYPTED: "DECRYPTED",
    VERIFIED: "VERIFIED",
    INVALID_CREDENTIALS: "INVALID_CREDENTIALS",
    INVALID_KEY: "INVALID_KEY",
    INVALID_CERTIFICATE: "INVALID_CERTIFICATE",
    INVALID_MESSAGE_INTEGRITY: "INVALID_MESSAGE_INTEGRITY",
    INVALID_PKCS7_ENVELOPE: "INVALID_PKCS7_ENVELOPE",
    INVALID_SIGNATURE: "INVALID_SIGNATURE",
    REVOKED_CREDENTIALS: "REVOKED_CREDENTIALS",
    EXPIRED_CREDENTIALS: "EXPIRED_CREDENTIALS",
    UNSUPPORTED_ALGORITHM: "UNSUPPORTED_ALGORITHM",
    UNTRUSTED_CREDENTIALS: "UNTRUSTED_CREDENTIALS",
    UNMATCHED_RECIPIENTS: "UNMATCHED_RECIPIENTS",
    UNKNOWN: "UNKNOWN"
};
export enum SMIMEPrefKeys {
    SIGNATURE = "sign_message_by_default",
    ENCRYPTION = "encrypt_message_by_default"
}
export const IS_SW_AVAILABLE = !!navigator.serviceWorker?.controller;
