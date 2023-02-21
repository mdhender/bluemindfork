import MimeType from "./MimeType";

export default {
    createAlternativePart,
    createAttachmentParts,
    createHtmlPart,
    createInlineImageParts,
    createMixedPart,
    createMultipartReport,
    createRelatedPart,
    createReportPart,
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

function createInlineImageParts(structure, inlineImages) {
    if (inlineImages && inlineImages.length > 0) {
        const childrenOfRelatedPart = [structure.children[1]].concat(inlineImages);
        structure.children[1] = createRelatedPart(childrenOfRelatedPart);
    }
    return structure;
}

function createReportPart(address, fileName) {
    return {
        mime: MimeType.MESSAGE_DISPOSITION_NOTIFICATION,
        address,
        encoding: "7bit",
        fileName,
        dispositionType: "ATTACHMENT"
    };
}

function createMultipartReport(textPart, reportPart) {
    return {
        mime: MimeType.MULTIPART_REPORT,
        children: [textPart, reportPart]
    };
}
