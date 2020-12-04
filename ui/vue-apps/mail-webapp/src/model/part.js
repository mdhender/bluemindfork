import { mailText2Html, MimeType } from "@bluemind/email";
import { html2text } from "@bluemind/html-utils";

export function setAddresses(structure, keepTmpAddress = false) {
    const tmpAddresses = [];
    structure.address = "TEXT";
    setAddressesForChildren(structure.children, keepTmpAddress, tmpAddresses);
    return tmpAddresses;
}

function setAddressesForChildren(children, keepTmpAddress, tmpAddresses, base = "") {
    children.forEach((part, index) => {
        const isAddressTemporary = isTemporaryPart(part);
        if (isAddressTemporary) {
            tmpAddresses.push(part.address);
        }
        if (!keepTmpAddress || !isAddressTemporary) {
            part.address = base ? base + "." + (index + 1) : index + 1 + "";
        }
        if (part.children) {
            setAddressesForChildren(part.children, keepTmpAddress, tmpAddresses, part.address);
        }
    });
}

function isTemporaryPart(part) {
    /*
     * if part is only uploaded, its address is an UID
     * if part is built in EML, its address is something like "1.1"
     * if address is not defined, it's a multipart so considered as built in EML
     */
    return part.address ? !/^([0-9]+)(\.[0-9]+)*$/.test(part.address) : false;
}

export function getPartsFromCapabilities(message, availableCapabilities) {
    const partsByCapabilities = message.inlinePartsByCapabilities.find(part =>
        part.capabilities.every(capability => availableCapabilities.includes(capability))
    );
    return partsByCapabilities ? partsByCapabilities.parts : [];
}

export function mergePartsForTextarea(message, partsToMerge, partsDataByAddress) {
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

export function mergePartsForRichEditor(message, partsToMerge, partsDataByAddress) {
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
