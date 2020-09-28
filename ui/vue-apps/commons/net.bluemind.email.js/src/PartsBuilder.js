import MimeType from "./MimeType";

export default {
    createAlternativePart,
    createAttachmentParts,
    createHtmlPart,
    createInlineImageParts,
    createMixedPart,
    createRelatedPart,
    createTextPart
};

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

function createInlineImageParts(structure, inlineImages, newAddresses) {
    if (inlineImages && inlineImages.length > 0) {
        inlineImages.forEach(part => (part.address = part.address ? part.address : newAddresses.pop()));
        const childrenOfRelatedPart = [structure.children[1]].concat(inlineImages);
        structure.children[1] = createRelatedPart(childrenOfRelatedPart);
    }
    return structure;
}
