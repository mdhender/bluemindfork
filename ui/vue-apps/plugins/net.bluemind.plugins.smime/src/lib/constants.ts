export const SMIME_INTERNAL_API_URL = "/service-worker-internal/smime";
export const PKCS7_MIMES = ["application/pkcs7-mime", "application/x-pkcs7-mime"];

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
export const CRYPTO_HEADER_NAME = "X-BM-Crypto";
export const CRYPTO_HEADERS = {
    IS_ENCRYPTED: "0",
    DECRYPTED: "1",
    INVALID_CREDENTIALS: "100",
    REVOKED_CREDENTIALS: "101",
    EXPIRED_CREDENTIALS: "102",
    UNTRUSTED_CREDENTIALS: "103",
    UNMATCHED_RECIPIENTS: "104"
};