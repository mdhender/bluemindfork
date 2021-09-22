import { html2text, sanitizeHtml } from "@bluemind/html-utils";
import { InlineImageHelper, PartsBuilder } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import random from "lodash.random";

import { isNewMessage } from "~/model/draft";
import { AttachmentStatus } from "~/model/attachment";
import { MessageHeader, MessageStatus } from "~/model/message";
import {
    RESET_PENDING_ATTACHMENTS,
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
} from "~/mutations";
import MessageAdaptor from "../helpers/MessageAdaptor";
import { FolderAdaptor } from "~/store/folders/helpers/FolderAdaptor";

export function isReadyToBeSaved(draft, messageCompose) {
    const checkAttachments =
        draft.attachments.every(a => a.status === AttachmentStatus.UPLOADED) ||
        (isNewMessage(draft) && messageCompose.pendingAttachments.length > 0); // due to attachments forward cases
    return (draft.status === MessageStatus.IDLE || draft.status === MessageStatus.NEW) && checkAttachments;
}

export async function save(context, draft, messageCompose) {
    if (!draft) {
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
        // eslint-disable-next-line no-console
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

    if (!headers.find(h => h.name.toUpperCase() === MessageHeader.MESSAGE_ID.toUpperCase())) {
        headers.push(generateMessageIDHeader(draft));
    }

    context.commit(SET_MESSAGE_HEADERS, { messageKey: draft.key, headers });
    context.commit(SET_MESSAGE_DATE, { messageKey: draft.key, date: saveDate });

    const remoteMessage = MessageAdaptor.toMailboxItem(draft, structure);
    const { imapUid, id: internalId } = isNewMessage(draft)
        ? await service.create(remoteMessage)
        : {
              ...(await service.updateById(draft.remoteRef.internalId, remoteMessage)),
              id: draft.remoteRef.internalId
          };
    context.commit(SET_MESSAGE_INTERNAL_ID, { key: draft.key, internalId });
    context.commit(SET_MESSAGE_IMAP_UID, { key: draft.key, imapUid });
    const mailItem = await inject("MailboxItemsPersistence", draft.folderRef.uid).getCompleteById(internalId);
    const adapted = MessageAdaptor.fromMailboxItem(mailItem, FolderAdaptor.toRef(draft.folderRef.uid));
    context.commit(SET_MESSAGE_INLINE_PARTS_BY_CAPABILITIES, {
        key: draft.key,
        inlinePartsByCapabilities: adapted.inlinePartsByCapabilities
    });
}

function generateMessageIDHeader(draft) {
    const encodedDate = new Date().getTime().toString(36);
    const encodedRandomNumber = random(Math.pow(2, 64) - 1).toString(36);
    const domain = draft.from.address.substring(draft.from.address.indexOf("@") + 1);
    const value = "<" + encodedDate + "." + encodedRandomNumber + "@" + domain + ">";
    return {
        name: MessageHeader.MESSAGE_ID,
        values: [value]
    };
}

// when attachments are forwarded, we have to upload them at first save
async function handleAttachmentsForForward(draft, service, messageCompose, commit) {
    if (
        isNewMessage(draft) &&
        messageCompose.pendingAttachments.length > 0 &&
        draft.attachments.length >= messageCompose.pendingAttachments.length
    ) {
        const addresses = await Promise.all(
            messageCompose.pendingAttachments.map(content => service.uploadPart(content))
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

        commit(RESET_PENDING_ATTACHMENTS);
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
