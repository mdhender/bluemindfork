import { html2text, sanitizeHtml } from "@bluemind/html-utils";
import { Flag, MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { partUtils } from "@bluemind/mail";
import store from "@bluemind/store";

const { sanitizeTextPartForCyrus } = partUtils;

import { draftUtils, messageUtils, signatureUtils } from "@bluemind/mail";
import { ADD_FLAG } from "~/actions";
import {
    SET_MESSAGE_DATE,
    SET_MESSAGE_HEADERS,
    SET_MESSAGE_INTERNAL_ID,
    SET_MESSAGE_IMAP_UID,
    SET_MESSAGES_STATUS,
    SET_SAVE_ERROR,
    SET_MESSAGE_SIZE,
    DELETE_FLAG
} from "~/mutations";
import { FolderAdaptor } from "~/store/folders/helpers/FolderAdaptor";
import { VCard } from "@bluemind/addressbook.api";
import { fetchMembersWithAddress } from "@bluemind/contact";
import { DISCLAIMER_SELECTOR } from "@bluemind/mail/src/signature";
import { cloneDeep } from "lodash";

const { isNewMessage } = draftUtils;
const { MessageAdaptor, MessageHeader, MessageStatus, generateMessageIDHeader } = messageUtils;
const { CORPORATE_SIGNATURE_PLACEHOLDER, CORPORATE_SIGNATURE_SELECTOR } = signatureUtils;

export function isReadyToBeSaved(draft) {
    return (
        draft.status === MessageStatus.IDLE ||
        draft.status === MessageStatus.NEW ||
        draft.status === MessageStatus.SAVE_ERROR
    );
}

export async function save(draft, messageCompose) {
    draft = { ...draft };
    const service = inject("MailboxItemsPersistence", draft.folderRef.uid);
    let htmlAddress, textAddress;
    try {
        store.commit(`mail/${SET_MESSAGES_STATUS}`, [{ key: draft.key, status: MessageStatus.SAVING }]);
        ({ htmlAddress, textAddress } = await prepareDraft(service, draft, messageCompose));

        const structure = updateStructure(draft.structure, textAddress, htmlAddress);

        await expandGroups(draft);
        await manageDispositionNotification(draft);
        await createEmlOnServer(draft, service, structure);

        store.commit(`mail/${SET_MESSAGES_STATUS}`, [{ key: draft.key, status: MessageStatus.IDLE }]);
        store.commit(`mail/${SET_SAVE_ERROR}`, null);
    } catch (err) {
        // eslint-disable-next-line no-console
        console.error(err);
        store.commit(`mail/${SET_MESSAGES_STATUS}`, [{ key: draft.key, status: MessageStatus.SAVE_ERROR }]);
        store.commit(`mail/${SET_SAVE_ERROR}`, err);
    } finally {
        [htmlAddress, textAddress].forEach(address => service.removePart(address));
    }
}

async function prepareDraft(service, draft, messageCompose) {
    let wholeContent = messageCompose.collapsedContent
        ? messageCompose.editorContent + messageCompose.collapsedContent
        : messageCompose.editorContent;
    wholeContent = removeCorporateSignatureContent(wholeContent, messageCompose);
    wholeContent = sanitizeHtml(wholeContent, true);

    const textHtml = sanitizeTextPartForCyrus(wholeContent);
    const textPlain = sanitizeTextPartForCyrus(html2text(wholeContent));
    store.commit(`mail/${SET_MESSAGE_SIZE}`, { key: draft.key, preview: textPlain });
    const tmpAddresses = await uploadParts(service, textPlain, textHtml);

    return tmpAddresses;
}

async function createEmlOnServer(draft, service, structure) {
    const { saveDate, headers } = forceMailRewriteOnServer(draft);

    if (!headers.find(h => h.name.toUpperCase() === MessageHeader.MESSAGE_ID.toUpperCase())) {
        headers.push(generateMessageIDHeader(draft.from.address));
    }

    store.commit(`mail/${SET_MESSAGE_HEADERS}`, { messageKey: draft.key, headers });
    store.commit(`mail/${SET_MESSAGE_DATE}`, { messageKey: draft.key, date: saveDate });

    const remoteMessage = MessageAdaptor.toMailboxItem({ ...draft, headers }, structure);
    const { imapUid, id: internalId } = isNewMessage(draft)
        ? await service.create(remoteMessage)
        : {
              ...(await service.updateById(draft.remoteRef.internalId, remoteMessage)),
              id: draft.remoteRef.internalId
          };
    store.commit(`mail/${SET_MESSAGE_INTERNAL_ID}`, { key: draft.key, internalId });
    store.commit(`mail/${SET_MESSAGE_IMAP_UID}`, { key: draft.key, imapUid });
    const mailItem = await inject("MailboxItemsPersistence", draft.folderRef.uid).getCompleteById(internalId);
    const adapted = MessageAdaptor.fromMailboxItem(mailItem, FolderAdaptor.toRef(draft.folderRef.uid));
    store.commit(`mail/${SET_MESSAGE_SIZE}`, { key: draft.key, size: adapted.size });
}

function removeCorporateSignatureContent(content, { corporateSignature, disclaimer }) {
    let html = content;
    if (corporateSignature || disclaimer) {
        const htlmDoc = new DOMParser().parseFromString(html, MimeType.TEXT_HTML);
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

async function uploadParts(service, textPlain, textHtml) {
    const promises = [];
    promises.push(service.uploadPart(textPlain));
    promises.push(service.uploadPart(textHtml));
    const [textAddress, htmlAddress] = await Promise.all(promises);
    return { textAddress, htmlAddress };
}

function updateStructure(existingStructure, textPlainAddress, textHtmlAddress) {
    const structure = cloneDeep(existingStructure);
    let alternativePart;
    if (existingStructure.mime === MimeType.MULTIPART_ALTERNATIVE) {
        alternativePart = structure;
    } else {
        alternativePart = structure.children.find(({ mime }) => mime === MimeType.MULTIPART_ALTERNATIVE);
    }

    let textPart = alternativePart.children.find(({ mime }) => mime === MimeType.TEXT_PLAIN);
    textPart.address = textPlainAddress;

    let htmlPart;
    const relatedPart = alternativePart.children.find(({ mime }) => mime === MimeType.MULTIPART_RELATED);
    let htmlParent = relatedPart ? relatedPart : alternativePart;
    htmlPart = htmlParent.children.find(({ mime }) => mime === MimeType.TEXT_HTML);

    htmlPart.address = textHtmlAddress;

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

async function manageDispositionNotification(draft) {
    const index = draft.headers.findIndex(
        header =>
            new RegExp(MessageHeader.DISPOSITION_NOTIFICATION_TO, "i").test(header.name) &&
            header.values?.filter(Boolean)?.length
    );
    if (index >= 0) {
        await store.dispatch(`mail/${ADD_FLAG}`, { messages: [draft], flag: Flag.MDN_SENT });
    } else {
        await store.commit(`mail/${DELETE_FLAG}`, { messages: [draft], flag: Flag.MDN_SENT });
    }
}
