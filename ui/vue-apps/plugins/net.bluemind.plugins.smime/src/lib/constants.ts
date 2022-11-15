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
    INVALID_KEY: "101",
    INVALID_CERTIFICATE: "102",
    REVOKED_CREDENTIALS: "103",
    EXPIRED_CREDENTIALS: "104",
    UNTRUSTED_CREDENTIALS: "105",
    UNMATCHED_RECIPIENTS: "106"
};
