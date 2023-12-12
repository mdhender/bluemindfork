import { InlineImageHelper, MimeType } from "@bluemind/email";
import { sanitizeHtml } from "@bluemind/html-utils";
import { BmRichEditor } from "@bluemind/ui-components";
import { draftUtils, loadingStatusUtils, messageUtils, partUtils } from "@bluemind/mail";
import store from "@bluemind/store";

import { FETCH_PART_DATA, FETCH_MESSAGE_IF_NOT_LOADED, SET_DRAFT_CONTENT } from "~/actions";
import {
    ADD_MESSAGES,
    SET_DRAFT_COLLAPSED_CONTENT,
    SET_MESSAGE_STRUCTURE,
    SET_MESSAGE_LOADING_STATUS
} from "~/mutations";
import apiMessages from "~/store/api/apiMessages";
import { getIdentityForNewMessage, setFrom } from "~/composables/composer/ComposerFrom";

import { useAddAttachmentsCommand, useForwardCommand } from "~/commands";
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
    const execForward = useForwardCommand();
    const { initForwardEml } = useForwardEml();
    const userPrefTextOnly = ref(false); // FIXME: https://forge.bluemind.net/jira/browse/FEATWEBML-88
    const partsByMessageKey = computed(() => store.state.mail.partsData.partsByMessageKey);

    // case when user clicks on a message in MY_DRAFTS folder
    async function initFromRemoteMessage(message) {
        const messageWithTmpAddresses = await apiMessages.getForUpdate(message);
        store.commit(`mail/${SET_MESSAGE_STRUCTURE}`, {
            messageKey: message.key,
            structure: messageWithTmpAddresses.structure
        });
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
            content = insertionResult.contentsWithImageInserted[0];
            content = sanitizeHtml(content, true);
        }

        const editorData = handleSeparator(content);
        store.commit(`mail/${SET_DRAFT_COLLAPSED_CONTENT}`, editorData.collapsed);
        store.dispatch(`mail/${SET_DRAFT_CONTENT}`, { html: editorData.content, draft: message });
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
            message.structure = buildBasicStructure();
            store.commit(`mail/${ADD_MESSAGES}`, { messages: [message] });
            store.commit(`mail/${SET_MESSAGE_LOADING_STATUS}`, {
                messageKey: message.key,
                status: LoadingStatus.LOADING
            });
            const previousInfos = await getPreviousInfos(previousMessage);

            switch (action) {
                case MessageCreationModes.REPLY:
                case MessageCreationModes.REPLY_ALL: {
                    return await initReplyOrForward(message, action, previousInfos);
                }
                case MessageCreationModes.FORWARD: {
                    const { message: newMessage } = await execForward({ message, previousInfos });
                    return newMessage;
                }
                case MessageCreationModes.EDIT_AS_NEW: {
                    return await initEditAsNew(message, previousInfos);
                }
                case MessageCreationModes.FORWARD_AS_EML: {
                    return await initForwardEml(message, previousInfos);
                }
                default:
                    return await initNewMessage(folder);
            }
        } catch {
            return await initNewMessage(folder);
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

        store.commit(`mail/${SET_DRAFT_COLLAPSED_CONTENT}`, null);
        store.dispatch(`mail/${SET_DRAFT_CONTENT}`, { html: body || BmRichEditor.constants.NEW_LINE, draft: message });
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
