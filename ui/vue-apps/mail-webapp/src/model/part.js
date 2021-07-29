import { mailText2Html, MimeType } from "@bluemind/email";
import { html2text } from "@bluemind/html-utils";

export function createFromFile(address, { name, type, size }) {
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

export function getPartsFromCapabilities(message, availableCapabilities) {
    const partsByCapabilities = message.inlinePartsByCapabilities.find(part =>
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
        const partContent = parts[part.address];
        if (MimeType.equals(part.mime, MimeType.TEXT_HTML)) {
            result += partContent;
        } else if (MimeType.equals(part.mime, MimeType.TEXT_PLAIN)) {
            result += mailText2Html(partContent, userLang);
        }
    }
    return result;
}
