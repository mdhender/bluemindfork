import { html2text, sanitizeHtml } from "@bluemind/html-utils";
import { Flag, InlineImageHelper, PartsBuilder } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { partUtils } from "@bluemind/mail";

const { sanitizeTextPartForCyrus } = partUtils;

import { draftUtils, fileUtils, messageUtils, signatureUtils } from "@bluemind/mail";
import { ADD_FLAG, DELETE_FLAG } from "~/actions";
import {
    MAX_MESSAGE_SIZE_EXCEEDED,
    SET_MESSAGE_DATE,
    SET_MESSAGE_HEADERS,
    SET_MESSAGE_INTERNAL_ID,
    SET_MESSAGE_IMAP_UID,
    SET_MESSAGES_STATUS,
    SET_SAVED_INLINE_IMAGES,
    SET_MESSAGE_INLINE_PARTS_BY_CAPABILITIES,
    SET_MESSAGE_PREVIEW,
    SET_MESSAGE_SIZE
} from "~/mutations";
import { FolderAdaptor } from "~/store/folders/helpers/FolderAdaptor";
import { VCard } from "@bluemind/addressbook.api";
import { fetchMembersWithAddress } from "@bluemind/contact";
import { DISCLAIMER_SELECTOR } from "@bluemind/mail/src/signature";

const { isNewMessage } = draftUtils;
const { FileStatus } = fileUtils;
const { MessageAdaptor, MessageHeader, MessageStatus, generateMessageIDHeader } = messageUtils;
const { CORPORATE_SIGNATURE_PLACEHOLDER, CORPORATE_SIGNATURE_SELECTOR } = signatureUtils;

export function isReadyToBeSaved(draft, files) {
    const attachmentsAreUploaded = files.every(f => f.status === FileStatus.UPLOADED);
    return (
        (draft.status === MessageStatus.IDLE ||
            draft.status === MessageStatus.NEW ||
            draft.status === MessageStatus.SAVE_ERROR) &&
        attachmentsAreUploaded
    );
}

export async function save(context, draft, messageCompose, files) {
    draft = { ...draft };
    const service = inject("MailboxItemsPersistence", draft.folderRef.uid);
    let tmpAddresses = [],
        inlineImages = [];
    try {
        context.commit(SET_MESSAGES_STATUS, [{ key: draft.key, status: MessageStatus.SAVING }]);
        ({ tmpAddresses, inlineImages } = await prepareDraft(context, service, draft, messageCompose));
        const structure = createDraftStructure(tmpAddresses[0], tmpAddresses[1], files, inlineImages);

        await expandGroups(draft);
        await manageDispositionNotification(context, draft);
        await createEmlOnServer(context, draft, service, structure);

        context.commit(SET_MESSAGES_STATUS, [{ key: draft.key, status: MessageStatus.IDLE }]);
        context.commit(MAX_MESSAGE_SIZE_EXCEEDED, false);
    } catch (err) {
        // eslint-disable-next-line no-console
        console.error(err);
        context.commit(SET_MESSAGES_STATUS, [{ key: draft.key, status: MessageStatus.SAVE_ERROR }]);
        if (err.data?.errorCode === "ENTITY_TOO_LARGE") {
            context.commit(MAX_MESSAGE_SIZE_EXCEEDED, true);
        }
    } finally {
        tmpAddresses.slice(0, 2).forEach(address => service.removePart(address));
    }
}

async function prepareDraft(context, service, draft, messageCompose) {
    let wholeContent = messageCompose.collapsedContent
        ? messageCompose.editorContent + messageCompose.collapsedContent
        : messageCompose.editorContent;
    wholeContent = removeCorporateSignatureContent(wholeContent, messageCompose);
    wholeContent = sanitizeHtml(wholeContent, true);

    const insertionResult = InlineImageHelper.insertCid(wholeContent, messageCompose.inlineImagesSaved);

    const textHtml = sanitizeTextPartForCyrus(insertionResult.htmlWithCids);
    const textPlain = sanitizeTextPartForCyrus(html2text(insertionResult.htmlWithCids));
    context.commit(SET_MESSAGE_PREVIEW, { key: draft.key, preview: textPlain });
    const tmpAddresses = await uploadParts(service, textPlain, textHtml, insertionResult.newContentByCid);

    const newInlineImages = insertionResult.newParts.map((part, index) => ({
        ...part,
        address: tmpAddresses[index + 2]
    }));
    const inlineImages = insertionResult.alreadySaved.concat(newInlineImages);
    context.commit(SET_SAVED_INLINE_IMAGES, inlineImages);

    return { tmpAddresses, inlineImages };
}

async function createEmlOnServer(context, draft, service, structure) {
    const inlinePartsByCapabilities = MessageAdaptor.computeParts(structure).inlinePartsByCapabilities;
    context.commit(SET_MESSAGE_INLINE_PARTS_BY_CAPABILITIES, { key: draft.key, inlinePartsByCapabilities });

    const { saveDate, headers } = forceMailRewriteOnServer(draft);

    if (!headers.find(h => h.name.toUpperCase() === MessageHeader.MESSAGE_ID.toUpperCase())) {
        headers.push(generateMessageIDHeader(draft.from.address));
    }

    context.commit(SET_MESSAGE_HEADERS, { messageKey: draft.key, headers });
    context.commit(SET_MESSAGE_DATE, { messageKey: draft.key, date: saveDate });

    const remoteMessage = MessageAdaptor.toMailboxItem({ ...draft, headers }, structure);
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
    context.commit(SET_MESSAGE_SIZE, { key: draft.key, size: adapted.size });
    context.commit(SET_MESSAGE_INLINE_PARTS_BY_CAPABILITIES, {
        key: draft.key,
        inlinePartsByCapabilities: adapted.inlinePartsByCapabilities
    });
}

function removeCorporateSignatureContent(content, { corporateSignature, disclaimer }) {
    let html = content;
    if (corporateSignature || disclaimer) {
        const htlmDoc = new DOMParser().parseFromString(html, "text/html");
        if (corporateSignature) {
            const element = htlmDoc.querySelector(CORPORATE_SIGNATURE_SELECTOR);
            if (element) {
                if (corporateSignature.usePlaceholder) {
                    element.replaceWith(CORPORATE_SIGNATURE_PLACEHOLDER);
                    html = htlmDoc.body.innerHTML;
                } else {
                    element.remove();
                    html = htlmDoc.body.innerHTML;
                }
            }
        }
        if (disclaimer) {
            const element = htlmDoc.querySelector(DISCLAIMER_SELECTOR);
            if (element) {
                element.remove();
                html = htlmDoc.body.innerHTML;
            }
        }
    }
    return html;
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

/** Recursively convert groups having no address to recipients with an address.  */
async function expandGroups(draft) {
    draft.to = await expandGroupRecipients(draft.to);
    draft.cc = await expandGroupRecipients(draft.cc);
    draft.bcc = await expandGroupRecipients(draft.bcc);
}

async function expandGroupRecipients(recipients) {
    const expanded = [];
    await Promise.all(
        recipients?.map(async recipient => {
            if (recipient.kind === VCard.Kind.group && !recipient.address) {
                expanded.push(...(await fetchMembersWithAddress(recipient.containerUid, recipient.uid)));
            } else {
                expanded.push(recipient);
            }
        })
    );
    return expanded;
}

async function manageDispositionNotification(context, draft) {
    const index = draft.headers.findIndex(
        header =>
            new RegExp(MessageHeader.DISPOSITION_NOTIFICATION_TO, "i").test(header.name) &&
            header.values?.filter(Boolean)?.length
    );
    if (index >= 0) {
        await context.dispatch(ADD_FLAG, { messages: [draft], flag: Flag.MDN_SENT });
    } else {
        await context.dispatch(DELETE_FLAG, { messages: [draft], flag: Flag.MDN_SENT });
    }
}
