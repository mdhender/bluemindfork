import { MessageBody } from "@bluemind/backend.mail.api";
import { CRYPTO_HEADERS, SIGNED_HEADER_NAME, ENCRYPTED_HEADER_NAME } from "./constants";

export function isSigned(headers: MessageBody.Header[]): boolean {
    return headers.some(header => header.name === SIGNED_HEADER_NAME);
}

export function isVerified(headers: MessageBody.Header[]): boolean {
    const value = findHeaderValue(headers, SIGNED_HEADER_NAME);
    return !!value && !!(parseInt(value) & CRYPTO_HEADERS.OK);
}

export function isEncrypted(headers: MessageBody.Header[]): boolean {
    return headers.some(header => header.name === ENCRYPTED_HEADER_NAME);
}

export function hasToBeEncrypted(headers: MessageBody.Header[]): boolean {
    const value = findHeaderValue(headers, ENCRYPTED_HEADER_NAME);
    return !!value && !!(parseInt(value) & CRYPTO_HEADERS.TO_DO);
}

export function hasToBeSigned(headers: MessageBody.Header[]): boolean {
    // TODO
    return true;
}

export function isDecrypted(headers: MessageBody.Header[]): boolean {
    const value = findHeaderValue(headers, ENCRYPTED_HEADER_NAME);
    return !!value && !!(parseInt(value) & CRYPTO_HEADERS.OK);
}

export function getDecryptHeader(headers: MessageBody.Header[]): string | null {
    return findHeaderValue(headers, ENCRYPTED_HEADER_NAME);
}

export function addHeaderValue(
    headers: MessageBody.Header[] = [],
    headerName: string,
    headerValue: number
): MessageBody.Header[] {
    const newHeaders = [...headers];
    const index = headers.findIndex(({ name }) => name === headerName);

    if (index === -1) {
        newHeaders.push({ name: headerName, values: [headerValue.toString()] });
    } else {
        const currentValues = headers[index].values || [];
        const newValue = parseInt(currentValues[0]) | headerValue;
        newHeaders[index] = { name: headerName, values: [newValue.toString()] };
    }
    return newHeaders;
}

export function removeHeader(headers: MessageBody.Header[] = [], headerName: string): MessageBody.Header[] {
    const newHeaders = [...headers];
    const index = headers.findIndex(({ name }) => name === headerName);

    if (index > -1) {
        newHeaders.splice(index, 1);
    }
    return newHeaders;
}

function findHeaderValue(headers: MessageBody.Header[], headerName: string): string | null {
    const cryptoHeader = headers.find(header => header.name === headerName);
    if (cryptoHeader && cryptoHeader.values) {
        return cryptoHeader?.values[0];
    }
    return null;
}
