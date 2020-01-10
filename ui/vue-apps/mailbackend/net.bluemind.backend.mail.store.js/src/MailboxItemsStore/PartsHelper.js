/** Utilities for body message parts handling. */
export default {
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

/**
 * @return true if we consider the given part should be attached,
 *         i.e.: not shown in the message body, false otherwise
 */
function isAttachment(part) {
    return part.dispositionType && part.dispositionType === "ATTACHMENT";
}
