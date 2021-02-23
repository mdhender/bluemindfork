import { mailText2Html, MimeType } from "@bluemind/email";
import { html2text } from "@bluemind/html-utils";

export function getPartsFromCapabilities(message, availableCapabilities) {
    const partsByCapabilities = message.inlinePartsByCapabilities.find(part =>
        part.capabilities.every(capability => availableCapabilities.includes(capability))
    );
    return partsByCapabilities ? partsByCapabilities.parts : [];
}

export function mergePartsForTextarea(partsToMerge, partsDataByAddress) {
    let result = "";
    for (const part of partsToMerge) {
        const partContent = partsDataByAddress[part.address];
        if (MimeType.equals(part.mime, MimeType.TEXT_PLAIN)) {
            result += partContent;
        } else if (MimeType.equals(part.mime, MimeType.TEXT_HTML)) {
            result += html2text(partContent);
        }
    }
    return result;
}

export function mergePartsForRichEditor(partsToMerge, partsDataByAddress) {
    let result = "";
    for (const part of partsToMerge) {
        const partContent = partsDataByAddress[part.address];
        if (MimeType.equals(part.mime, MimeType.TEXT_HTML)) {
            result += partContent;
        } else if (MimeType.equals(part.mime, MimeType.TEXT_PLAIN)) {
            result += mailText2Html(partContent);
        }
    }
    return result;
}
