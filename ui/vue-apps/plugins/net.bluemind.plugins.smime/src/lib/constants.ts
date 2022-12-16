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
export enum CRYPTO_HEADERS {
    DECRYPTED = 1,
    VERIFIED = 1,
    INVALID_KEY = 2,
    INVALID_CERTIFICATE = 4,
    REVOKED_CERTIFICATE = 8,
    EXPIRED_CERTIFICATE = 16,
    UNSUPPORTED_ALGORITHM = 32,
    UNTRUSTED_CERTIFICATE = 64,
    UNMATCHED_RECIPIENTS = 128,
    INVALID_MESSAGE_INTEGRITY = 256,
    INVALID_PKCS7_ENVELOPE = 512,
    INVALID_SIGNATURE = 1024,
    DECRYPT_FAILURE = 2048,
    ENCRYPT_FAILURE = 4096,
    UNKNOWN = 0
}
export enum SMIMEPrefKeys {
    SIGNATURE = "sign_message_by_default",
    ENCRYPTION = "encrypt_message_by_default"
}
export const IS_SW_AVAILABLE = !!navigator.serviceWorker?.controller;
