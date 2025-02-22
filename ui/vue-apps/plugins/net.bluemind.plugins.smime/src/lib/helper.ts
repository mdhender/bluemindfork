import { MessageBody } from "@bluemind/backend.mail.api";
import { MimeType } from "@bluemind/email";
import { CRYPTO_HEADERS, SIGNED_HEADER_NAME, ENCRYPTED_HEADER_NAME } from "./constants";

export function hasSignatureHeader(headers: MessageBody.Header[] = []): boolean {
    return headers.some(header => header.name === SIGNED_HEADER_NAME);
}

export function isVerified(headers: MessageBody.Header[] = []): boolean {
    return matchSignedHeaderValue(headers, CRYPTO_HEADERS.OK);
}

export function hasEncryptionHeader(headers: MessageBody.Header[] = []): boolean {
    return headers.some(header => header.name === ENCRYPTED_HEADER_NAME);
}

export function hasToBeEncrypted(headers: MessageBody.Header[] = []): boolean {
    return matchEncryptedHeaderValue(headers, CRYPTO_HEADERS.TO_DO);
}

export function hasToBeSigned(headers: MessageBody.Header[] = []): boolean {
    return matchSignedHeaderValue(headers, CRYPTO_HEADERS.TO_DO);
}

export function isDecrypted(headers: MessageBody.Header[] = []): boolean {
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

export function isEncrypted(part: MessageBody.Part): boolean {
    return MimeType.isPkcs7(part);
}

export function isSigned(part: MessageBody.Part): boolean {
    return part.mime === MimeType.PKCS_7_SIGNED_DATA || (!!part.children && part.children.some(isSigned));
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
        const value = parseInt(currentValues[0]);
        if (!(value & headerValue)) {
            const newValue = value | headerValue;
            newHeaders[index] = { name: headerName, values: [newValue.toString()] };
        }
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
        newHeaders[index] = {
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

export function removeSignatureFromStructure(structure: MessageBody.Part | undefined): MessageBody.Part {
    let newStructure = { ...structure };
    if (newStructure.children) {
        const signatureIndex = newStructure.children.findIndex(
            ({ mime }: MessageBody.Part) => mime === MimeType.PKCS_7_SIGNED_DATA
        );

        if (signatureIndex > -1) {
            newStructure.children.splice(signatureIndex, 1);
            if (newStructure.children.length === 1) {
                newStructure = newStructure.children[0];
            }
        }
    }
    return newStructure;
}
