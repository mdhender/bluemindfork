import { MessageBody } from "@bluemind/backend.mail.api";
import { CRYPTO_HEADERS, SIGNED_HEADER_NAME, ENCRYPTED_HEADER_NAME } from "./constants";

export function isSigned(headers: MessageBody.Header[]): boolean {
    return headers.some(header => header.name === SIGNED_HEADER_NAME);
}

export function isVerified(headers: MessageBody.Header[]): boolean {
    return matchSignedHeaderValue(headers, CRYPTO_HEADERS.OK);
}

export function hasEncryptionHeader(headers: MessageBody.Header[]): boolean {
    return headers.some(header => header.name === ENCRYPTED_HEADER_NAME);
}

export function hasToBeEncrypted(headers: MessageBody.Header[]): boolean {
    return matchEncryptedHeaderValue(headers, CRYPTO_HEADERS.TO_DO);
}

export function hasToBeSigned(headers: MessageBody.Header[]): boolean {
    return matchSignedHeaderValue(headers, CRYPTO_HEADERS.TO_DO);
}

export function isDecrypted(headers: MessageBody.Header[]): boolean {
    return matchEncryptedHeaderValue(headers, CRYPTO_HEADERS.OK);
}

export function getHeaderValue(headers: MessageBody.Header[] = [], headerName: string): number | null {
    const header = headers.find(header => header.name === headerName);
    if (header && header.values) {
        const int = parseInt(header.values[0]);
        return isNaN(int) ? null : int;
    }
    return null;
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

export function resetHeader(headers: MessageBody.Header[] = [], headerName: string) {
    const newHeaders = [...headers];
    const index = headers.findIndex(({ name }) => name === headerName);
    if (index > -1) {
        const hadTodo = matchEncryptedHeaderValue(headers, CRYPTO_HEADERS.TO_DO);
        headers[index] = {
            name: ENCRYPTED_HEADER_NAME,
            values: hadTodo ? [CRYPTO_HEADERS.TO_DO.toString()] : []
        };
    }
    return newHeaders;
}

function matchEncryptedHeaderValue(headers: MessageBody.Header[] = [], headervalue: number) {
    return matchHeaderValue(headers, ENCRYPTED_HEADER_NAME, headervalue);
}
function matchSignedHeaderValue(headers: MessageBody.Header[] = [], headervalue: number) {
    return matchHeaderValue(headers, SIGNED_HEADER_NAME, headervalue);
}

function matchHeaderValue(headers: MessageBody.Header[] = [], headerName: string, headervalue: number) {
    const value = getHeaderValue(headers, headerName);
    return !!value && !!(value & headervalue);
}
