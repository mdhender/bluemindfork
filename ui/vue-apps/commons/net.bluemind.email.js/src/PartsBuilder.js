import MimeType from "./MimeType";

export default {
    createAlternativePart,
    createAttachmentPart,
    createAttachmentParts,
    createCalendarRequestPart,
    createHtmlPart,
    createInlineImageParts,
    createInlinePart,
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

function createAlternativePart(...parts) {
    return {
        mime: MimeType.MULTIPART_ALTERNATIVE,
        children: parts
    };
}

function createRelatedPart(children) {
    return {
        mime: MimeType.MULTIPART_RELATED,
        children: children
    };
}

function createInlinePart({ address, size, mime, cid }) {
    return {
        address,
        mime,
        dispositionType: "INLINE",
        encoding: "base64",
        contentId: cid,
        size
    };
}

function createInlineImageParts(structure, inlineImages) {
    if (inlineImages && inlineImages.length > 0) {
        const childrenOfRelatedPart = [structure.children[1]].concat(inlineImages);
        structure.children[1] = createRelatedPart(childrenOfRelatedPart);
    }
    return structure;
}

function createCalendarRequestPart(address) {
    return {
        mime: MimeType.TEXT_CALENDAR,
        address,
        encoding: "quoted-printable",
        charset: "utf-8",
        headers: [{ name: "Content-Type", values: [`${MimeType.TEXT_CALENDAR}; charset=UTF-8; method=REQUEST`] }]
    };
}

function createAttachmentPart({ mime, address, fileName, size }) {
    return {
        address,
        charset: "us-ascii",
        dispositionType: "ATTACHMENT",
        encoding: "base64",
        fileName,
        mime,
        size
    };
}
