export default function (eml: string): { toDigest: string; pkcs7Part: ArrayBuffer } {
    const newLine = "\r\n";
    const iMultipartLine = eml.indexOf("Content-Type: multipart/signed");
    const iBoundary = eml.indexOf("boundary", iMultipartLine);
    const endOfMultipartLine = eml.indexOf(newLine, iMultipartLine);
    const boundaryValue = "--" + eml.substring(iBoundary + 'boundary="'.length, endOfMultipartLine - 1);

    const { toDigest, beginOfPkcs7 } = extractContentToDigest(eml, boundaryValue, endOfMultipartLine);
    const pkcs7Part = extractSignedPart(eml, beginOfPkcs7, boundaryValue);
    return { toDigest, pkcs7Part };
}

function extractContentToDigest(eml: string, boundary: string, startIndex: number) {
    const newLine = "\r\n";
    const partBegin = eml.indexOf(boundary, startIndex) + boundary.length + newLine.length;
    const partEnd = eml.indexOf(boundary, partBegin) - newLine.length; // remove trailing CRLF
    const beginOfPkcs7 = partEnd + newLine.length + boundary.length + newLine.length;
    return { toDigest: eml.substring(partBegin, partEnd), beginOfPkcs7 };
}

function extractSignedPart(eml: string, beginOfPkcs7: number, boundary: string) {
    const newLine = "\r\n";
    let tmpIndex = beginOfPkcs7;
    while (eml.startsWith("Content-", tmpIndex) || eml.startsWith(newLine, tmpIndex)) {
        tmpIndex = eml.indexOf(newLine, tmpIndex) + newLine.length;
    }
    const start = tmpIndex - newLine.length;
    tmpIndex = eml.indexOf(boundary, tmpIndex);

    let end = tmpIndex;
    while (eml[end - 1] + eml[end] === newLine) {
        end = end - newLine.length;
    }
    const base64 = eml.substring(start, end);
    return base64ToArrayBuffer(base64);
}

function base64ToArrayBuffer(base64: string): ArrayBuffer {
    const binary_string = atob(base64);
    const len = binary_string.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i++) {
        bytes[i] = binary_string.charCodeAt(i);
    }
    return bytes.buffer;
}
