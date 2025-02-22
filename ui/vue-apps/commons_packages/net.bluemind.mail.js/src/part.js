import { mailText2Html, MimeType } from "@bluemind/email";
import { html2text } from "@bluemind/html-utils";

export function createFromFile({ address, name, type, size }) {
    // default encoding and charset set by server
    return {
        address,
        charset: "us-ascii",
        fileName: name,
        encoding: "base64",
        mime: type || "application/octet-stream",
        size
    };
}

export function getPartsFromCapabilities({ inlinePartsByCapabilities }, availableCapabilities) {
    const partsByCapabilities = inlinePartsByCapabilities.find(part =>
        part.capabilities.every(capability =>
            availableCapabilities.some(available => capability.startsWith(available) || available === capability)
        )
    );
    return partsByCapabilities ? partsByCapabilities.parts : [];
}

export function mergePartsForTextarea(partsToMerge, parts) {
    let result = "";
    for (const part of partsToMerge) {
        const partContent = parts[part.address];
        if (MimeType.equals(part.mime, MimeType.TEXT_PLAIN)) {
            result += partContent;
        } else if (MimeType.equals(part.mime, MimeType.TEXT_HTML)) {
            result += html2text(partContent);
        }
    }
    return result;
}

export function mergePartsForRichEditor(partsToMerge, parts, userLang) {
    let result = "";
    for (const part of partsToMerge) {
        const partContent = parts ? parts[part.address] : "";
        if (MimeType.equals(part.mime, MimeType.TEXT_HTML)) {
            result += partContent;
        } else if (MimeType.equals(part.mime, MimeType.TEXT_PLAIN)) {
            result += mailText2Html(partContent, userLang);
        }
    }
    return result;
}

export function isViewable({ mime }) {
    return VIEWER_CAPABILITIES.some(available => mime.startsWith(available));
}

export function isLeaf(part) {
    return !part.children?.length && !MimeType.isMultipart(part);
}

export const VIEWER_CAPABILITIES = [
    MimeType.AUDIO,
    MimeType.IMAGE,
    MimeType.MESSAGE,
    MimeType.PDF,
    MimeType.VIDEO,
    MimeType.TEXT_PLAIN,
    MimeType.TEXT_HTML,
    MimeType.X509_CERT,
    MimeType.CRYPTO_CERT,
    MimeType.PEM_FILE
];

export function sanitizeTextPartForCyrus(text) {
    return text.replace(/\r?\n/g, "\r\n");
}

export function hasCalendarPart(node) {
    if (MimeType.isCalendar(node)) {
        return true;
    }
    return node.children?.some(hasCalendarPart);
}

export default {
    createFromFile,
    getPartsFromCapabilities,
    isViewable,
    isLeaf,
    mergePartsForRichEditor,
    mergePartsForTextarea,
    sanitizeTextPartForCyrus,
    VIEWER_CAPABILITIES,
    hasCalendarPart
};
