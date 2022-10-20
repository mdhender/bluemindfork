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
