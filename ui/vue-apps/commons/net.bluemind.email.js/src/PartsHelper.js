import { inject } from "@bluemind/inject";
import UUIDGenerator from "@bluemind/uuid";

import MimeType from "./MimeType";

export default {
    createAlternativePart,
    createAttachmentParts,
    createHtmlPart,
    createInlineImageParts,
    createMixedPart,
    createRelatedPart,
    createTextPart,
    fetch,
    insertCid,
    insertInlineImages,
    isAttachment
};

/**
 * Replace the CID references found in partsWithReferences by the corresponding images found in imageParts.
 *
 * @param partsWithReferences parts with CID references inside an image markup
 * @param imageParts parts with base64 images and a CID identifier
 */
function insertInlineImages(partsWithReferences = [], imageParts = []) {
    const inlineReferenceRegex = /<img[^>]+?src\s*=\s*['"]cid:([^'"]*)['"][^>]*?>{1}?/gim;
    const inlined = [];
    partsWithReferences.forEach(partWithReferences => {
        let inlineReferences;
        while ((inlineReferences = inlineReferenceRegex.exec(partWithReferences.content)) !== null) {
            const cid = inlineReferences[1];
            const replaceRegex = new RegExp("(<img[^>]+?src\\s*=\\s*['\"])cid:" + cid + "(['\"][^>]*?>{1}?)", "gmi");
            const imagePart = imageParts.find(
                part => part.contentId && part.contentId.toUpperCase() === "<" + inlineReferences[1].toUpperCase() + ">"
            );
            if (imagePart) {
                partWithReferences.content = partWithReferences.content.replace(
                    replaceRegex,
                    "$1" + URL.createObjectURL(imagePart.content) + "$2"
                );
                inlined.push(imagePart.contentId);
            }
        }
    });
    return inlined;
}

function insertCid(html) {
    const images = [];
    const imageTags = new DOMParser().parseFromString(html, "text/html").querySelectorAll("img[src]");

    imageTags.forEach(img => {
        const dataSrc = img.src;
        if (dataSrc.startsWith("data:image")) {
            const cid = UUIDGenerator.generate() + "@bluemind.net";
            html = html.replace(dataSrc, "cid:" + cid);

            const extractDataRegex = /data:image(.*)base64,/g;
            const metadatas = dataSrc.match(extractDataRegex)[0];
            const data = dataSrc.replace(metadatas, "");
            images.push({
                content: convertData(data),
                part: {
                    mime: metadatas.substring(5, metadatas.length - 8),
                    dispositionType: "INLINE",
                    encoding: "base64",
                    contentId: cid
                }
            });
        }
    });
    return { images, html };
}

function convertData(b64Data) {
    const byteCharacters = atob(b64Data);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    return new Uint8Array(byteNumbers);
}

/**
 * @return true if we consider the given part should be attached,
 *         i.e.: not shown in the message body, false otherwise
 */
function isAttachment(part) {
    return part.dispositionType && part.dispositionType === "ATTACHMENT";
}

function createAttachmentParts(attachments, structure) {
    if (attachments.length > 0) {
        let children = [structure];
        children.push(...attachments.filter(a => a.status !== "ERROR"));
        structure = createMixedPart(children);
    }
    return structure;
}

function createMixedPart(children) {
    return {
        mime: MimeType.MULTIPART_MIXED,
        children
    };
}

function createHtmlPart(address) {
    return {
        mime: MimeType.TEXT_HTML,
        address,
        encoding: "quoted-printable",
        charset: "utf-8"
    };
}

function createTextPart(address) {
    return {
        mime: MimeType.TEXT_PLAIN,
        address,
        encoding: "quoted-printable",
        charset: "utf-8"
    };
}

function createAlternativePart(textPart, htmlPart) {
    return {
        mime: MimeType.MULTIPART_ALTERNATIVE,
        children: [textPart, htmlPart]
    };
}

function createRelatedPart(children) {
    return {
        mime: MimeType.MULTIPART_RELATED,
        children: children
    };
}

function createInlineImageParts(structure, addresses, images) {
    if (images && images.length > 0) {
        const childrenOfRelatedPart = [structure.children[1]];
        images.forEach((image, index) => {
            const inlineImageChildPart = Object.assign({}, image.part, { address: addresses[index] });
            childrenOfRelatedPart.push(inlineImageChildPart);
        });
        structure.children[1] = createRelatedPart(childrenOfRelatedPart);
    }
    return structure;
}

// FIXME: duplicated code with fetch action. Remove fetch action once MailViewer is refactored
async function fetch(messageImapUid, folderUid, part, isAttachment) {
    const stream = await inject("MailboxItemsPersistence", folderUid).fetch(
        messageImapUid,
        part.address,
        part.encoding,
        part.mime,
        part.charset
    );
    if (!isAttachment && (MimeType.isText(part) || MimeType.isHtml(part) || MimeType.isCalendar(part))) {
        return new Promise(resolve => {
            const reader = new FileReader();
            reader.readAsText(stream, part.encoding);
            reader.addEventListener("loadend", e => {
                resolve(e.target.result);
            });
        });
    } else {
        return stream;
    }
}
