import { computed } from "vue";
import { BmRichEditor } from "@bluemind/ui-components";
import { MimeType, InlineImageHelper } from "@bluemind/email";
import { messageUtils, draftUtils, partUtils } from "@bluemind/mail";
import { sanitizeHtml } from "@bluemind/html-utils";
import store from "@bluemind/store";
import { getIdentityForReplyOrForward, setFrom } from "../ComposerFrom";
import { buildForwardStructure, buildReplyStructure } from "./initStructure";
import {
    SET_MESSAGE_HEADERS,
    SET_MESSAGE_SUBJECT,
    SET_MESSAGE_TO,
    SET_MESSAGE_CC,
    SET_MESSAGE_STRUCTURE,
    SET_DRAFT_COLLAPSED_CONTENT
} from "~/mutations";
import { FETCH_PART_DATA, SET_DRAFT_CONTENT } from "~/actions";
const { MessageCreationModes, MessageHeader } = messageUtils;
const {
    getEditorContent,
    computeToRecipients,
    handleIdentificationFields,
    computeCcRecipients,
    quotePreviousMessage,
    computeSubject,
    COMPOSER_CAPABILITIES
} = draftUtils;

const { getPartsFromCapabilities } = partUtils;

export default async function initReplyOrForward(message, creationMode, previous, userPrefTextOnly) {
    const {
        message: previousMessage,
        attachments: previousAttachments,
        inlinePartsByCapabilities: previousInlinePartsByCapabilities
    } = previous;
    const previousInlines = getPartsFromCapabilities(
        { inlinePartsByCapabilities: previousInlinePartsByCapabilities },
        COMPOSER_CAPABILITIES
    );
    const identity = getIdentityForReplyOrForward(previousMessage);

    const identificationHeaders = handleIdentificationFields(previousMessage);
    const draftHeader = createDraftHeader(creationMode, previousMessage);
    store.commit(`mail/${SET_MESSAGE_HEADERS}`, {
        messageKey: message.key,
        headers: [draftHeader, ...identificationHeaders]
    });
    const subject = computeSubject(creationMode, previousMessage);
    store.commit(`mail/${SET_MESSAGE_SUBJECT}`, { messageKey: message.key, subject: subject });

    if (creationMode === MessageCreationModes.REPLY_ALL || creationMode === MessageCreationModes.REPLY) {
        const to = computeToRecipients(creationMode, previousMessage, identity);
        store.commit(`mail/${SET_MESSAGE_TO}`, { messageKey: message.key, to });
        const cc = computeCcRecipients(creationMode, previousMessage, identity)?.filter(
            contact => !message.to.some(to => to.address === contact.address)
        );
        store.commit(`mail/${SET_MESSAGE_CC}`, { messageKey: message.key, cc });
    }

    createEditorContent(creationMode, userPrefTextOnly, previousMessage, previousInlines, message);
    await setFrom(identity, message);
    const structure = buildMessageStructure(creationMode, previousInlines, previousAttachments);
    store.commit(`mail/${SET_MESSAGE_STRUCTURE}`, { messageKey: message.key, structure });

    return message;
}

function createDraftHeader(creationMode, { remoteRef, folderRef }) {
    const draftInfoHeader = {
        type: creationMode,
        messageInternalId: remoteRef.internalId,
        folderUid: folderRef.uid
    };
    return { name: MessageHeader.X_BM_DRAFT_INFO, values: [JSON.stringify(draftInfoHeader)] };
}

function buildMessageStructure(creationMode, inlines, attachments) {
    return creationMode === MessageCreationModes.FORWARD
        ? buildForwardStructure(inlines, attachments)
        : buildReplyStructure(inlines);
}

async function createEditorContent(creationMode, userPrefTextOnly, previousMessage, inlines, newMessage) {
    await store.dispatch(`mail/${FETCH_PART_DATA}`, {
        messageKey: previousMessage.key,
        folderUid: previousMessage.folderRef.uid,
        imapUid: previousMessage.remoteRef.imapUid,
        parts: inlines.filter(
            part => MimeType.isHtml(part) || MimeType.isText(part) || (MimeType.isImage(part) && part.contentId)
        )
    });
    const partsByMessageKey = computed(() => store.state.mail.partsData.partsByMessageKey);

    let contentFromPreviousMessage = getEditorContent(
        userPrefTextOnly,
        inlines,
        partsByMessageKey.value[previousMessage.key],
        store.state.settings.lang
    );

    if (!userPrefTextOnly) {
        const partsWithCid = inlines.filter(part => MimeType.isImage(part) && part.contentId);
        const insertionResult = await InlineImageHelper.insertAsBase64(
            [contentFromPreviousMessage],
            partsWithCid,
            partsByMessageKey.value[previousMessage.key]
        );
        contentFromPreviousMessage = insertionResult.contentsWithImageInserted[0];
        contentFromPreviousMessage = sanitizeHtml(contentFromPreviousMessage, true);
    }
    const collapsed = quotePreviousMessage(contentFromPreviousMessage, previousMessage, creationMode, userPrefTextOnly);
    store.commit(`mail/${SET_DRAFT_COLLAPSED_CONTENT}`, collapsed);
    store.dispatch(`mail/${SET_DRAFT_CONTENT}`, {
        html: BmRichEditor.constants.NEW_LINE + collapsed,
        draft: newMessage
    });
}
