import { CRYPTO_HEADERS, SIGNED_HEADER_NAME, ENCRYPTED_HEADER_NAME } from "./constants";

export function isSigned(headers) {
    const cryptoHeader = headers.find(header => header.name === SIGNED_HEADER_NAME);
    return !!cryptoHeader;
}

export function isVerified(headers) {
    const cryptoHeader = headers.find(header => header.name === SIGNED_HEADER_NAME);
    return cryptoHeader?.values.find(value => value === CRYPTO_HEADERS.VERIFIED);
}

export function isEncrypted(headers) {
    return headers.find(header => header.name === ENCRYPTED_HEADER_NAME);
}
