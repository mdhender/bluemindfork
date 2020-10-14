import { MessageCreationModes, MessageStatus, createOnlyMetadata, messageKey } from "~/model/message";
import { ConversationListFilter } from "~/store/conversationList";
import { Flag } from "@bluemind/email";
import { FolderAdaptor } from "~/store/folders/helpers/FolderAdaptor";
import { LoadingStatus } from "./loading-status";
import { draftInfoHeader } from "~/model/draft";
import { isDraftFolder } from "~/model/folder";

export function createConversationStubsFromRawConversations(rawConversations, activeFolder) {
    return rawConversations
        .map(({ value: { messageRefs, conversationId } }) => {
            const sortedMessageRefs = messageRefs.sort((a, b) => a.date - b.date);
            const last = sortedMessageRefs[sortedMessageRefs.length - 1];
            const metadata = createConversationMetadata({
                internalId: conversationId,
                folder: FolderAdaptor.toRef(activeFolder.uid)
            });
            metadata.conversationId = conversationId;
            metadata.date = last.date;
            metadata.messages = sortedMessageRefs.map(m =>
                createOnlyMetadata({
                    internalId: m.itemId,
                    folder: FolderAdaptor.toRef(m.folderUid),
                    conversationRef: { key: metadata.key, id: conversationId }
                })
            );
            return metadata;
        })
        .sort((a, b) => b.date - a.date);
}

export function createConversationStubsFromSortedIds(sortedIds, folder) {
    return sortedIds.map(id => {
        const conversation = createConversationMetadata({ internalId: id, folder: FolderAdaptor.toRef(folder) });
        conversation.messages = [
            {
                ...conversation,
                conversationRef: { key: conversation.key, id: conversation.id }
            }
        ];
        return conversation;
    });
}

export function createConversationStubsFromSearchResult(searchResult) {
    return searchResult.map(({ id, folderRef }) => {
        const conversation = createConversationMetadata({ internalId: id, folder: folderRef });
        conversation.messages = [
            {
                ...conversation,
                conversationRef: { key: conversation.key, id: conversation.id }
            }
        ];
        return conversation;
    });
}

function createConversationMetadata({ internalId, folder: { key, uid } }) {
    return {
        key: internalId && key ? messageKey(internalId, key) : null,
        folderRef: { key, uid },
        remoteRef: { internalId },
        status: MessageStatus.IDLE,
        loading: LoadingStatus.NOT_LOADED
    };
}

export function sameConversation(a, b) {
    if (a.folderRef.key !== b.folderRef.key) {
        return false;
    }
    const messagesA = a.messages;
    const messagesB = b.messages;
    if (messagesA.length !== messagesB.length) {
        return false;
    }
    const intersection = messagesA.filter(messageA => messagesB.map(messageB => messageB.key).includes(messageA.key));
    return intersection.length === messagesA.length;
}

export function firstMessageInConversationFolder(getters, conversations) {
    return conversations.map(conversation =>
        getters.CONVERSATION_MESSAGE_BY_KEY(conversation.key).find(m => m.folderRef.key === conversation.folderRef.key)
    );
}

export function messagesInConversationFolder(getters, conversations) {
    return conversations.flatMap(conversation =>
        getters
            .CONVERSATION_MESSAGE_BY_KEY(conversation.key)
            .filter(m => m.folderRef.key === conversation.folderRef.key)
    );
}

/** @return true if each given conversation has more than one item in 'messages' property.  */
export function conversationsOnly(conversations) {
    return conversations.every(({ messages }) => messages.length > 1);
}

export function firstMessageFolderKey(conversation) {
    return conversation.messages[0].folderRef.key;
}

export function matchFilter(conversation, filter) {
    switch (filter) {
        case ConversationListFilter.ALL:
            return true;
        case ConversationListFilter.UNREAD:
            return !conversation.flags.includes(Flag.SEEN);
        case ConversationListFilter.FLAGGED:
            return conversation.flags.includes(Flag.FLAGGED);
        default:
            return false;
    }
}

/**
 * Sort by date, ascending, except for draft which should be just after its related message. */
export function sortConversationMessages(messages, folders) {
    // sort by date
    const sorted = messages.sort((a, b) => a.date - b.date);

    // search for drafts
    const drafts = messages.filter(m => isDraftFolder(folders[m.folderRef.key].path));
    const allowedCreationModes = [
        MessageCreationModes.REPLY_ALL,
        MessageCreationModes.REPLY,
        MessageCreationModes.FORWARD
    ];
    drafts.forEach(c => {
        const draftInfo = draftInfoHeader(c);
        if (
            draftInfo &&
            allowedCreationModes.includes(draftInfo.type) &&
            draftInfo.messageInternalId &&
            draftInfo.folderUid
        ) {
            const relatedIndex = sorted.findIndex(
                s => s.remoteRef.internalId === draftInfo.messageInternalId && s.folderRef.uid === draftInfo.folderUid
            );
            const draftIndex = sorted.findIndex(s => s.key === c.key);
            // remove
            const draft = sorted.splice(draftIndex, 1)[0];
            // insert
            sorted.splice(relatedIndex + 1, 0, draft);
        }
    });

    return sorted;
}

/**
 * Remove duplicates in Sent (keep those in INBOX, need to be loaded).
 */
export function removeSentDuplicates(messages, sentFolder) {
    const messageKeysByMessageId = {};
    messages.forEach(message => {
        if (message.loading !== LoadingStatus.NOT_LOADED) {
            if (message.messageId) {
                const messageKeys = messageKeysByMessageId[message.messageId] || [];
                messageKeysByMessageId[message.messageId] = [...messageKeys, message.key];
            }
        }
    });

    const arrayOfMessageKeys = Object.values(messageKeysByMessageId);
    return messages.filter(
        f =>
            f.folderRef.key !== sentFolder.key ||
            !arrayOfMessageKeys.some(keys => keys.length > 1 && keys.includes(f.key))
    );
}
