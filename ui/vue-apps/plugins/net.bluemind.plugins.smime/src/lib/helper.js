import { CRYPTO_HEADERS, SIGNED_HEADER_NAME, ENCRYPTED_HEADER_NAME } from "./constants";

export function isSigned(headers) {
    return headers.some(header => header.name === SIGNED_HEADER_NAME);
}

export function isVerified(headers) {
    const cryptoHeader = headers.find(header => header.name === SIGNED_HEADER_NAME);
    return cryptoHeader?.values.find(value => value === CRYPTO_HEADERS.VERIFIED);
}

export function isEncrypted(headers) {
    return headers.some(header => header.name === ENCRYPTED_HEADER_NAME);
}

export function isDecrypted(headers) {
    const cryptoHeader = headers.find(header => header.name === ENCRYPTED_HEADER_NAME);
    return cryptoHeader?.values.find(value => value === CRYPTO_HEADERS.DECRYPTED);
}
