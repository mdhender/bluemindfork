import { PartsBuilder } from "@bluemind/email";
import { inject } from "@bluemind/inject";

export async function createForwardStructure(folder, calendarAddress, attachments) {
    const service = inject("MailboxItemsPersistence", folder.remoteRef.uid);

    const [textPlainAddress, textHtmlAddress] = await Promise.all([service.uploadPart(""), service.uploadPart("")]);

    let structure;

    const textPart = PartsBuilder.createTextPart(textPlainAddress);
    const htmlPart = PartsBuilder.createHtmlPart(textHtmlAddress);
    const calendarPart = PartsBuilder.createCalendarPart(calendarAddress);
    structure = PartsBuilder.createAlternativePart(textPart, htmlPart, calendarPart);
    structure = PartsBuilder.createAttachmentParts(attachments, structure); // TODO refactor to separate mixed part and attachments

    return structure;
}
