import { mailText2Html, MimeType } from "@bluemind/email";
import { html2text } from "@bluemind/html-utils";

export function setAddresses(structure) {
    structure.address = "TEXT";
    setAddressesForChildren(structure.children);
}

function setAddressesForChildren(children, base = "") {
    children
        .filter(part => !isTemporaryPart(part))
        .forEach((part, index) => {
            part.address = base ? base + "." + (index + 1) : index + 1 + "";
            if (part.children) {
                setAddressesForChildren(part.children, part.address);
            }
        });
}

export function isTemporaryPart(part) {
    /*
     * if part is only uploaded, its address is an UID
     * if part is built in EML, its address is something like "1.1"
     * if address is not defined, it's a multipart so considered as built in EML
     */
    return part.address ? !/^([0-9]+)(\.[0-9]+)*$/.test(part.address) : false;
}

export function getPartsFromCapabilities(message, availableCapabilities) {
    return message.inlinePartsByCapabilities.find(part =>
        part.capabilities.every(capability => availableCapabilities.includes(capability))
    ).parts;
}

export function mergePartsForTextarea(message, partsToMerge) {
    let result = "";
    for (const part of partsToMerge) {
        const partContent = message.partContentByAddress[part.address];
        if (MimeType.equals(part.mime, MimeType.TEXT_PLAIN)) {
            result += partContent;
        } else if (MimeType.equals(part.mime, MimeType.TEXT_HTML)) {
            result += html2text(partContent);
        }
    }
    return result;
}

export function mergePartsForRichEditor(message, partsToMerge) {
    let result = "";
    for (const part of partsToMerge) {
        const partContent = message.partContentByAddress[part.address];
        if (MimeType.equals(part.mime, MimeType.TEXT_HTML)) {
            result += partContent;
        } else if (MimeType.equals(part.mime, MimeType.TEXT_PLAIN)) {
            result += mailText2Html(partContent);
        }
    }
    return result;
}
