import { MessageBody } from "@bluemind/backend.mail.api";
import { MimeType, PartsBuilder } from "@bluemind/email";

export function buildForwardStructure(inlines: MessageBody.Part[] = [], attachments: MessageBody.Part[] = []) {
    return withInlinesAndAttachments(inlines, attachments);
}

export function buildReplyStructure(inlines: MessageBody.Part[] = []) {
    return withInlines(inlines);
}
export function buildBasicStructure() {
    const emptyTextPart = PartsBuilder.createTextPart(null);
    const emptyHtmlPart = PartsBuilder.createHtmlPart(null);
    return PartsBuilder.createAlternativePart(emptyTextPart, emptyHtmlPart);
}
export function buildEditAsNewStructure(inlines: MessageBody.Part[] = [], attachments: MessageBody.Part[] = []) {
    return withInlinesAndAttachments(inlines, attachments);
}

function withInlines(inlines: MessageBody.Part[]) {
    let structure = buildBasicStructure();
    const images = inlines.filter(part => MimeType.isImage(part) && part.contentId);
    structure = PartsBuilder.createInlineImageParts(structure, images);
    return structure;
}

function withInlinesAndAttachments(inlines: MessageBody.Part[], attachments: MessageBody.Part[]) {
    let structure = withInlines(inlines);
    structure = PartsBuilder.createAttachmentParts(
        attachments.map(att => PartsBuilder.createAttachmentPart(att)),
        structure
    );
    return structure;
}
