import { base64ToArrayBuffer } from "@bluemind/arraybuffer";

export default function (eml: string): { toDigest: string; pkcs7Part: ArrayBuffer } {
    const boundaryValue = extractBoundary(eml);
    const { toDigest, beginOfPkcs7 } = extractContentToDigest(eml, boundaryValue);
    const pkcs7Part = extractSignedPart(eml, beginOfPkcs7, boundaryValue);
    return { toDigest, pkcs7Part };
}

function extractContentToDigest(eml: string, boundary: string) {
    const newLine = "\r\n";
    const startIndex = eml.indexOf(boundary + boundary.length);
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

// INTERNAL METHOD (exported only for testing purpose)
export function extractBoundary(eml: string): string {
    const boundaryTag = "boundary=";
    const iMultipartLine = eml.indexOf("Content-Type: multipart/signed");
    const iBoundary = eml.indexOf(boundaryTag, iMultipartLine);
    const startOfBoundary = iBoundary + boundaryTag.length;
    let boundaryValue;
    if (eml[startOfBoundary] === '"') {
        const endOfBoundary = eml.indexOf('"', startOfBoundary + 1);
        boundaryValue = eml.substring(startOfBoundary + 1, endOfBoundary);
    } else {
        const endOfLine = eml.indexOf("\n", startOfBoundary);
        const line = eml.substring(startOfBoundary, endOfLine + 1);
        for (const sep of [";", " ", "\n"]) {
            if (line.indexOf(sep) > -1) {
                boundaryValue = line.substring(0, line.indexOf(sep));
                break;
            }
        }
    }
    if (!boundaryValue) {
        throw "No boundary found for multipart/signed";
    }
    return "--" + boundaryValue.trim();
}
