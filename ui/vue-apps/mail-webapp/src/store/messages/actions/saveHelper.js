import { html2text, sanitizeHtml } from "@bluemind/html-utils";
import { InlineImageHelper, PartsBuilder } from "@bluemind/email";
import { inject } from "@bluemind/inject";

import { isNewMessage } from "~model/draft";
import { AttachmentStatus } from "~model/attachment";
import { MessageHeader, MessageStatus } from "~model/message";
import {
    RESET_ATTACHMENTS_FORWARDED,
    SET_MESSAGE_DATE,
    SET_MESSAGE_HEADERS,
    SET_MESSAGE_INTERNAL_ID,
    SET_MESSAGE_IMAP_UID,
    SET_MESSAGES_STATUS,
    SET_SAVED_INLINE_IMAGES,
    SET_MESSAGE_INLINE_PARTS_BY_CAPABILITIES,
    SET_ATTACHMENT_ADDRESS,
    SET_ATTACHMENT_STATUS,
    SET_MESSAGE_PREVIEW
} from "~mutations";
import MessageAdaptor from "../helpers/MessageAdaptor";

export function isReadyToBeSaved(draft, messageCompose) {
    const checkAttachments =
        draft.attachments.every(a => a.status === AttachmentStatus.UPLOADED) ||
        (isNewMessage(draft) && messageCompose.forwardedAttachments.length > 0); // due to attachments forward cases
    return draft.status === MessageStatus.IDLE && checkAttachments;
}

export async function save(context, draft, messageCompose) {
    if (!draft || draft.status === MessageStatus.REMOVED) {
        return;
    }
    try {
        context.commit(SET_MESSAGES_STATUS, [{ key: draft.key, status: MessageStatus.SAVING }]);

        const service = inject("MailboxItemsPersistence", draft.folderRef.uid);
        const { addresses, inlineImages } = await prepareDraft(context, service, draft, messageCompose);
        const structure = createDraftStructure(addresses[0], addresses[1], draft.attachments, inlineImages);
        await createEmlOnServer(context, draft, service, structure);
        addresses.slice(0, 2).forEach(address => service.removePart(address));

        context.commit(SET_MESSAGES_STATUS, [{ key: draft.key, status: MessageStatus.IDLE }]);
    } catch (e) {
        console.error(e);
        context.commit(SET_MESSAGES_STATUS, [{ key: draft.key, status: MessageStatus.SAVE_ERROR }]);
    }
}

async function prepareDraft(context, service, draft, messageCompose) {
    let editorContent = messageCompose.collapsedContent
        ? messageCompose.editorContent + messageCompose.collapsedContent
        : messageCompose.editorContent;
    editorContent = sanitizeHtml(editorContent);

    const insertionResult = InlineImageHelper.insertCid(editorContent, messageCompose.inlineImagesSaved);

    const textHtml = sanitizeForCyrus(insertionResult.htmlWithCids);
    const textPlain = sanitizeForCyrus(html2text(insertionResult.htmlWithCids));
    context.commit(SET_MESSAGE_PREVIEW, { key: draft.key, preview: textPlain });
    const addresses = await uploadParts(service, textPlain, textHtml, insertionResult.newContentByCid);

    const newInlineImages = insertionResult.newParts.map((part, index) => ({
        ...part,
        address: addresses[index + 2]
    }));
    const inlineImages = insertionResult.alreadySaved.concat(newInlineImages);
    context.commit(SET_SAVED_INLINE_IMAGES, inlineImages);

    await handleAttachmentsForForward(draft, service, messageCompose, context.commit);

    return { addresses, inlineImages };
}

async function createEmlOnServer(context, draft, service, structure) {
    const inlinePartsByCapabilities = MessageAdaptor.computeParts(structure).inlinePartsByCapabilities;
    context.commit(SET_MESSAGE_INLINE_PARTS_BY_CAPABILITIES, { key: draft.key, inlinePartsByCapabilities });

    const { saveDate, headers } = forceMailRewriteOnServer(draft);
    context.commit(SET_MESSAGE_HEADERS, { messageKey: draft.key, headers });
    context.commit(SET_MESSAGE_DATE, { messageKey: draft.key, date: saveDate });

    const remoteMessage = MessageAdaptor.toMailboxItem(draft, structure);
    if (isNewMessage(draft)) {
        const { imapUid, id: internalId } = await service.create(remoteMessage);
        context.commit(SET_MESSAGE_INTERNAL_ID, { key: draft.key, internalId });
        context.commit(SET_MESSAGE_IMAP_UID, { key: draft.key, imapUid });
    } else {
        const { imapUid } = await service.updateById(draft.remoteRef.internalId, remoteMessage);
        context.commit(SET_MESSAGE_IMAP_UID, { key: draft.key, imapUid });
    }
}

// when attachments are forwarded, we have to upload them at first save
async function handleAttachmentsForForward(draft, service, messageCompose, commit) {
    if (
        isNewMessage(draft) &&
        messageCompose.forwardedAttachments.length > 0 &&
        draft.attachments.length >= messageCompose.forwardedAttachments.length
    ) {
        const addresses = await Promise.all(
            messageCompose.forwardedAttachments.map(content => service.uploadPart(content))
        );

        addresses.forEach((newAddress, index) => {
            const attachment = draft.attachments[index];
            commit(SET_ATTACHMENT_ADDRESS, {
                messageKey: draft.key,
                oldAddress: attachment.address,
                address: newAddress
            });
            commit(SET_ATTACHMENT_STATUS, {
                messageKey: draft.key,
                address: newAddress,
                status: AttachmentStatus.UPLOADED
            });
        });

        commit(RESET_ATTACHMENTS_FORWARDED);
    }
}

function uploadParts(service, textPlain, textHtml, newContentByCid) {
    const promises = [];
    promises.push(service.uploadPart(textPlain));
    promises.push(service.uploadPart(textHtml));
    promises.push(...Object.values(newContentByCid).map(imgContent => service.uploadPart(imgContent)));
    return Promise.all(promises);
}

function createDraftStructure(textPlainAddress, textHtmlAddress, attachments, inlineImages) {
    let structure;

    const textPart = PartsBuilder.createTextPart(textPlainAddress);
    const htmlPart = PartsBuilder.createHtmlPart(textHtmlAddress);
    structure = PartsBuilder.createAlternativePart(textPart, htmlPart);
    structure = PartsBuilder.createInlineImageParts(structure, inlineImages);
    structure = PartsBuilder.createAttachmentParts(attachments, structure);

    return structure;
}

function sanitizeForCyrus(text) {
    return text.replace(/\r?\n/g, "\r\n");
}

/**
 * Needed by BM core to detect if mail has changed when using IMailboxItems.updateById
 */
function forceMailRewriteOnServer(draft) {
    const headers = JSON.parse(JSON.stringify(draft.headers));
    const saveDate = new Date();

    const hasXBmDraftKeyHeader = headers.find(header => header.name === MessageHeader.X_BM_DRAFT_REFRESH_DATE);
    if (hasXBmDraftKeyHeader) {
        hasXBmDraftKeyHeader.values = [saveDate.getTime()];
    } else {
        headers.push({
            name: MessageHeader.X_BM_DRAFT_REFRESH_DATE,
            values: [saveDate.getTime()]
        });
    }

    return { saveDate, headers };
}
