import { MessageBody } from "@bluemind/backend.mail.api";
import { CRYPTO_HEADERS, SIGNED_HEADER_NAME, ENCRYPTED_HEADER_NAME } from "./constants";

export function isSigned(headers: MessageBody.Header[]): boolean {
    return headers.some(header => header.name === SIGNED_HEADER_NAME);
}

export function isVerified(headers: MessageBody.Header[]): boolean {
    const value = findHeaderValue(headers, SIGNED_HEADER_NAME);
    return !!value && !!(parseInt(value) & CRYPTO_HEADERS.VERIFIED);
}

export function isEncrypted(headers: MessageBody.Header[]): boolean {
    return headers.some(header => header.name === ENCRYPTED_HEADER_NAME);
}

export function isDecrypted(headers: MessageBody.Header[]): boolean {
    const value = findHeaderValue(headers, ENCRYPTED_HEADER_NAME);
    return !!value && !!(parseInt(value) & CRYPTO_HEADERS.DECRYPTED);
}

export function getDecryptHeader(headers: MessageBody.Header[]): string | null {
    return findHeaderValue(headers, ENCRYPTED_HEADER_NAME);
}

export function binaryToArrayBuffer(binarysSring: string): ArrayBuffer {
    const len = binarysSring.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i++) {
        bytes[i] = binarysSring.charCodeAt(i);
    }
    return bytes.buffer;
}

export function base64ToArrayBuffer(base64: string): ArrayBuffer {
    const binary_string = atob(base64);
    return binaryToArrayBuffer(binary_string);
}

function findHeaderValue(headers: MessageBody.Header[], headerName: string): string | null {
    const cryptoHeader = headers.find(header => header.name === headerName);
    if (cryptoHeader && cryptoHeader.values) {
        return cryptoHeader?.values[0];
    }
    return null;
}
