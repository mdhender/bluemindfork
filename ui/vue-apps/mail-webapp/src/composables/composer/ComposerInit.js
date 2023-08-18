import { InlineImageHelper, MimeType } from "@bluemind/email";
import { sanitizeHtml } from "@bluemind/html-utils";
import { BmRichEditor } from "@bluemind/ui-components";
import { draftUtils, loadingStatusUtils, messageUtils, partUtils } from "@bluemind/mail";
import store from "@bluemind/store";

import { FETCH_PART_DATA, FETCH_MESSAGE_IF_NOT_LOADED } from "~/actions";
import {
    ADD_MESSAGES,
    SET_DRAFT_COLLAPSED_CONTENT,
    SET_DRAFT_EDITOR_CONTENT,
    SET_MESSAGE_STRUCTURE,
    SET_MESSAGE_LOADING_STATUS,
    SET_SAVED_INLINE_IMAGES
} from "~/mutations";
import apiMessages from "~/store/api/apiMessages";
import { getIdentityForNewMessage, setFrom } from "~/composables/composer/ComposerFrom";

import { useAddAttachmentsCommand } from "~/commands";
import { setForwardEventStructure } from "./forwardEvent";
import { computed, ref } from "vue";
import { buildBasicStructure } from "./init/initStructure";
import initReplyOrForward from "./init/initReplyOrForward";
import initEditAsNew from "./init/initEditAsNew";
import useForwardEml from "./init/initForwardEml";

const { COMPOSER_CAPABILITIES, createEmpty, getEditorContent, handleSeparator } = draftUtils;
const { getPartsFromCapabilities } = partUtils;
const { MessageCreationModes, computeParts } = messageUtils;
const { LoadingStatus } = loadingStatusUtils;
/**
 * Manage different cases of composer initialization
 */
export function useComposerInit() {
    const { execAddAttachments } = useAddAttachmentsCommand();
    const { initForwardEml } = useForwardEml();
    const userPrefTextOnly = ref(false); // FIXME: https://forge.bluemind.net/jira/browse/FEATWEBML-88
    const partsByMessageKey = computed(() => store.state.mail.partsData.partsByMessageKey);

    // case when user clicks on a message in MY_DRAFTS folder
    async function initFromRemoteMessage(message) {
        const messageWithTmpAddresses = await apiMessages.getForUpdate(message);

        const messageParts = computeParts(messageWithTmpAddresses.structure);
        const parts = getPartsFromCapabilities(messageParts, COMPOSER_CAPABILITIES);
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
        let message;
        try {
            const previousMessage = await store.dispatch(`mail/${FETCH_MESSAGE_IF_NOT_LOADED}`, {
                internalId: related.internalId,
                folder: store.state.mail.folders[related.folderKey]
            });

            message = createEmpty(folder);
            if (needsConversationRef(action)) {
                message.conversationRef = previousMessage.conversationRef;
            }
            store.commit(`mail/${ADD_MESSAGES}`, { messages: [message] });
            store.commit(`mail/${SET_MESSAGE_LOADING_STATUS}`, {
                messageKey: message.key,
                status: LoadingStatus.LOADING
            });
            const previousInfos = await getPreviousInfos(previousMessage);

            switch (action) {
                case MessageCreationModes.REPLY:
                case MessageCreationModes.REPLY_ALL:
                case MessageCreationModes.FORWARD: {
                    return initReplyOrForward(message, action, previousInfos);
                }
                case MessageCreationModes.FORWARD_EVENT: {
                    const newMessage = await initReplyOrForward(message, MessageCreationModes.FORWARD, previousInfos);
                    return setForwardEventStructure(previousInfos, newMessage);
                }
                case MessageCreationModes.EDIT_AS_NEW: {
                    return initEditAsNew(message, previousInfos);
                }
                case MessageCreationModes.FORWARD_AS_EML: {
                    return initForwardEml(message, previousInfos);
                }
                default:
                    return initNewMessage(folder);
            }
        } catch {
            return initNewMessage(folder);
        } finally {
            store.commit(`mail/${SET_MESSAGE_LOADING_STATUS}`, {
                messageKey: message.key,
                status: LoadingStatus.LOADED
            });
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
        store.commit(`mail/${SET_MESSAGE_STRUCTURE}`, {
            messageKey: message.key,
            structure: buildBasicStructure()
        });
        const identity = getIdentityForNewMessage();
        await setFrom(identity, message);
        store.commit(`mail/${SET_DRAFT_EDITOR_CONTENT}`, body || BmRichEditor.constants.NEW_LINE);

        store.commit(`mail/${SET_DRAFT_COLLAPSED_CONTENT}`, null);
        store.commit(`mail/${SET_SAVED_INLINE_IMAGES}`, []);
        return message;
    }

    return {
        initFromRemoteMessage,
        initRelatedMessage,
        initNewMessage,
        execAddAttachments
    };
}

function needsConversationRef(action) {
    return (
        [MessageCreationModes.REPLY, MessageCreationModes.REPLY_ALL].includes(action) &&
        store.state.mail.mailThreadSetting === "true"
    );
}

async function getPreviousInfos(message) {
    const previousWithTmpAddresses = await apiMessages.getForUpdate(message);
    const { attachments, inlinePartsByCapabilities } = computeParts(previousWithTmpAddresses.structure);

    return {
        message: previousWithTmpAddresses,
        attachments,
        inlinePartsByCapabilities
    };
}
