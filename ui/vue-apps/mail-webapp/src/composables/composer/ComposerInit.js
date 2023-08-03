import { ERROR } from "@bluemind/alert.store";
import { InlineImageHelper, MimeType } from "@bluemind/email";
import i18n from "@bluemind/i18n";
import { sanitizeHtml } from "@bluemind/html-utils";
import { BmRichEditor } from "@bluemind/ui-components";
import { attachmentUtils, draftUtils, loadingStatusUtils, messageUtils, partUtils } from "@bluemind/mail";
import store from "@bluemind/store";
import router from "@bluemind/router";

import { FETCH_PART_DATA, FETCH_MESSAGE_IF_NOT_LOADED } from "~/actions";
import { MY_DRAFTS } from "~/getters";
import {
    ADD_FILES,
    ADD_MESSAGES,
    SET_ATTACHMENTS,
    SET_DRAFT_COLLAPSED_CONTENT,
    SET_DRAFT_EDITOR_CONTENT,
    SET_MESSAGE_STRUCTURE,
    SET_MESSAGE_SUBJECT,
    SET_MESSAGE_TMP_ADDRESSES,
    SET_MESSAGES_LOADING_STATUS,
    SET_SAVED_INLINE_IMAGES
} from "~/mutations";
import apiMessages from "~/store/api/apiMessages";
import { getIdentityForNewMessage, setFrom, getIdentityForReplyOrForward } from "~/composables/composer/ComposerFrom";

import { useAddAttachmentsCommand } from "~/commands";
import { createForwardStructure } from "./forwardEvent";
import { computed, ref } from "vue";
import { useComposerMerge } from "./ComposerMerge";

const { LoadingStatus } = loadingStatusUtils;
const {
    COMPOSER_CAPABILITIES,
    computeSubject,
    createEmpty,
    createReplyOrForward,
    getEditorContent,
    handleSeparator,
    quotePreviousMessage
} = draftUtils;
const { getPartsFromCapabilities } = partUtils;
const { AttachmentAdaptor } = attachmentUtils;
const { MessageCreationModes } = messageUtils;

/**
 * Manage different cases of composer initialization
 */
export function useComposerInit() {
    const { maxSize, execAddAttachments } = useAddAttachmentsCommand();
    const { mergeBody, mergeAttachments, mergeSubject, mergeRecipients, mergeHeaders } = useComposerMerge();
    const userPrefTextOnly = ref(false); // FIXME: https://forge.bluemind.net/jira/browse/FEATWEBML-88
    const partsByMessageKey = computed(() => store.state.mail.partsData.partsByMessageKey);

    // case when user clicks on a message in MY_DRAFTS folder
    async function initFromRemoteMessage(message) {
        const messageWithTmpAddresses = await apiMessages.getForUpdate(message);
        const { files, attachments } = AttachmentAdaptor.extractFiles(messageWithTmpAddresses.attachments, message);
        store.commit(`mail/${ADD_FILES}`, { files });
        store.commit(`mail/${SET_MESSAGE_TMP_ADDRESSES}`, {
            key: message.key,
            attachments: attachments,
            inlinePartsByCapabilities: messageWithTmpAddresses.inlinePartsByCapabilities
        });
        const parts = getPartsFromCapabilities(messageWithTmpAddresses, COMPOSER_CAPABILITIES);

        await store.dispatch(`mail/${FETCH_PART_DATA}`, {
            messageKey: message.key,
            folderUid: message.folderRef.uid,
            imapUid: message.remoteRef.imapUid,
            parts: parts.filter(part => MimeType.isHtml(part) || MimeType.isText(part))
        });

        let content = getEditorContent(
            userPrefTextOnly.value,
            parts,
            partsByMessageKey.value[message.key],
            store.state.settings.lang
        );
        if (!userPrefTextOnly.value) {
            const partsWithCid = parts.filter(part => MimeType.isImage(part) && part.contentId);

            const insertionResult = await InlineImageHelper.insertAsUrl(
                [content],
                partsWithCid,
                message.folderRef.uid,
                message.remoteRef.imapUid
            );
            store.commit(`mail/${SET_SAVED_INLINE_IMAGES}`, insertionResult.imageInlined);
            content = insertionResult.contentsWithImageInserted[0];
            content = sanitizeHtml(content, true);
        }

        const editorData = handleSeparator(content);
        store.commit(`mail/${SET_DRAFT_COLLAPSED_CONTENT}`, editorData.collapsed);
        store.commit(`mail/${SET_DRAFT_EDITOR_CONTENT}`, editorData.content);
    }

    async function initRelatedMessage(folder, action, related) {
        try {
            const fetchRelatedFn = () =>
                store.dispatch(`mail/${FETCH_MESSAGE_IF_NOT_LOADED}`, {
                    internalId: related.internalId,
                    folder: store.state.mail.folders[related.folderKey]
                });
            switch (action) {
                case MessageCreationModes.REPLY:
                case MessageCreationModes.REPLY_ALL:
                case MessageCreationModes.FORWARD: {
                    const previous = await fetchRelatedFn();
                    return initReplyOrForward(folder, action, previous);
                }
                case MessageCreationModes.FORWARD_EVENT: {
                    const previous = await fetchRelatedFn();
                    return initForwardEvent(folder, previous);
                }
                case MessageCreationModes.EDIT_AS_NEW: {
                    const previous = await fetchRelatedFn();
                    return initEditAsNew(folder, previous);
                }
                case MessageCreationModes.FORWARD_AS_EML: {
                    const previous = await fetchRelatedFn();
                    return initForwardEml(previous);
                }
                default:
                    return initNewMessage(folder);
            }
        } catch {
            return initNewMessage(folder);
        }
    }

    // case of a new message
    async function initNewMessage(folder, { to = [], cc = [], bcc = [], subject = "", body }) {
        const message = createEmpty(folder);
        message.to = to;
        message.cc = cc;
        message.bcc = bcc;
        message.subject = subject;
        store.commit(`mail/${ADD_MESSAGES}`, { messages: [message] });
        const identity = getIdentityForNewMessage();
        await setFrom(identity, message);
        store.commit(`mail/${SET_DRAFT_EDITOR_CONTENT}`, body || BmRichEditor.constants.NEW_LINE);

        store.commit(`mail/${SET_DRAFT_COLLAPSED_CONTENT}`, null);
        store.commit(`mail/${SET_SAVED_INLINE_IMAGES}`, []);
        return message;
    }

    // case of a reply or forward message
    async function initReplyOrForward(folder, creationMode, previousMessage) {
        const identity = getIdentityForReplyOrForward(previousMessage);

        const message = createReplyOrForward(previousMessage, folder, creationMode, identity);

        if (creationMode !== MessageCreationModes.FORWARD && store.state.mail.mailThreadSetting === "true") {
            message.conversationRef = { ...previousMessage.conversationRef };
        }

        store.commit(`mail/${ADD_MESSAGES}`, { messages: [message] });

        await setFrom(identity, message);

        const parts = getPartsFromCapabilities(previousMessage, COMPOSER_CAPABILITIES);

        await store.dispatch(`mail/${FETCH_PART_DATA}`, {
            messageKey: previousMessage.key,
            folderUid: previousMessage.folderRef.uid,
            imapUid: previousMessage.remoteRef.imapUid,
            parts: parts.filter(
                part => MimeType.isHtml(part) || MimeType.isText(part) || (MimeType.isImage(part) && part.contentId)
            )
        });

        let contentFromPreviousMessage = getEditorContent(
            userPrefTextOnly.value,
            parts,
            partsByMessageKey.value[previousMessage.key],
            store.state.settings.lang
        );

        if (!userPrefTextOnly.value) {
            const partsWithCid = parts.filter(part => MimeType.isImage(part) && part.contentId);
            const insertionResult = await InlineImageHelper.insertAsBase64(
                [contentFromPreviousMessage],
                partsWithCid,
                partsByMessageKey.value[previousMessage.key]
            );
            contentFromPreviousMessage = insertionResult.contentsWithImageInserted[0];
            contentFromPreviousMessage = sanitizeHtml(contentFromPreviousMessage, true);
        }
        const collapsed = quotePreviousMessage(
            contentFromPreviousMessage,
            previousMessage,
            creationMode,
            userPrefTextOnly.value,
            i18n
        );

        store.commit(`mail/${SET_DRAFT_EDITOR_CONTENT}`, BmRichEditor.constants.NEW_LINE);
        store.commit(`mail/${SET_DRAFT_COLLAPSED_CONTENT}`, collapsed);
        store.commit(`mail/${SET_SAVED_INLINE_IMAGES}`, []);

        if (creationMode === MessageCreationModes.FORWARD) {
            copyAttachments(previousMessage, message);
        }

        store.commit("mail/" + SET_MESSAGES_LOADING_STATUS, [{ key: message.key, loading: LoadingStatus.LOADED }]);

        return message;
    }

    async function initForwardEvent(folder, previous) {
        const message = await initReplyOrForward(folder, MessageCreationModes.FORWARD, previous);

        const messageWithTmpAddresses = await apiMessages.getForUpdate(previous);
        const calendarPartAddress = getPartsFromCapabilities(messageWithTmpAddresses, [MimeType.TEXT_CALENDAR])?.[0]
            ?.address;
        const attachments = previous.attachments.map(({ fileKey }) => store.state.mail.files[fileKey]);

        const structure = await createForwardStructure(folder, calendarPartAddress, attachments);
        store.commit(`mail/${SET_MESSAGE_STRUCTURE}`, { messageKey: message.key, structure });
        return message;
    }

    async function initEditAsNew(folder, related) {
        const message = createEmpty(folder);
        store.commit(`mail/${ADD_MESSAGES}`, { messages: [message] });
        const identity = getIdentityForNewMessage();
        await setFrom(identity, message);
        mergeRecipients(message, related);
        mergeSubject(message, related);
        await mergeBody(message, related);
        await mergeAttachments(message, related);
        mergeHeaders(message, related);
        router.navigate({ name: "v:mail:message", params: { message: message } });
        return message;
    }

    async function initForwardEml(related) {
        const message = createEmpty(store.getters[`mail/${MY_DRAFTS}`]);
        store.commit(`mail/${ADD_MESSAGES}`, { messages: [message] });
        const identity = getIdentityForNewMessage();
        await setFrom(identity, message);
        const subject = computeSubject(MessageCreationModes.FORWARD, related);
        store.commit(`mail/${SET_MESSAGE_SUBJECT}`, { messageKey: message.key, subject });
        try {
            const content = await apiMessages.fetchComplete(related);
            const file = new File([content], messageUtils.createEmlName(related, i18n.t("mail.viewer.no.subject")), {
                type: "message/rfc822"
            });
            await execAddAttachments({ files: [file], message, maxSize: maxSize.value });
        } catch {
            store.dispatch(`alert/${ERROR}`, {
                alert: { name: "mail.forward_eml.fetch", uid: "FWD_EML_UID" }
            });
            const conversation = store.state.mail.conversations.conversationByKey[related.conversationRef.key];
            router.navigate({ name: "v:mail:conversation", params: { conversation } });
            return;
        }
        return message;
    }

    return {
        initFromRemoteMessage,
        initRelatedMessage,
        initNewMessage,
        initReplyOrForward,
        execAddAttachments
    };
}

async function copyAttachments(sourceMessage, destinationMessage) {
    const messageWithTmpAddresses = await apiMessages.getForUpdate(sourceMessage);
    const { files, attachments } = AttachmentAdaptor.extractFiles(messageWithTmpAddresses.attachments, sourceMessage);
    store.commit(`mail/${SET_ATTACHMENTS}`, { messageKey: destinationMessage.key, attachments });
    store.commit(`mail/${ADD_FILES}`, { files });
}
