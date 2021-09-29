import sortedIndexBy from "lodash.sortedindexby";

import { MessageCreationModes, MessageStatus, createOnlyMetadata, messageKey } from "~/model/message";
import { FolderAdaptor } from "~/store/folders/helpers/FolderAdaptor";
import { LoadingStatus } from "./loading-status";
import { draftInfoHeader } from "~/model/draft";
import { isDraftFolder } from "~/model/folder";

export function createConversationStubsFromRawConversations(rawConversations, folder) {
    const conversations = [],
        messages = [];
    rawConversations.forEach(({ value: { messageRefs }, uid }) => {
        const conversation = createConversationMetadata(uid, FolderAdaptor.toRef(folder), messageRefs);
        const index = sortedIndexBy(conversations, conversation, c => -c.date);
        conversations.splice(index, 0, conversation);
        messages.push(
            ...messageRefs.map(({ itemId: internalId, folderUid, date }) => {
                const folder = FolderAdaptor.toRef(folderUid);
                return createOnlyMetadata({
                    internalId,
                    folder,
                    conversationRef: { key: conversation.key, uid },
                    date
                });
            })
        );
    });
    return { conversations, messages };
}

export function createConversationStubsFromSortedIds(sortedIds, folder) {
    const conversations = [],
        messages = [];
    sortedIds.forEach(id => {
        const folderRef = FolderAdaptor.toRef(folder);
        const messageRef = { itemId: id, folderUid: folder.remoteRef.uid };
        const conversation = createConversationMetadata(id, folderRef, [messageRef]);
        const conversationRef = { key: conversation.key, id };
        const message = createOnlyMetadata({ internalId: id, folder: folderRef, conversationRef });
        conversations.push(conversation);
        messages.push(message);
    });
    return { conversations, messages };
}

export function createConversationStubsFromSearchResult(searchResult) {
    const conversations = [],
        messages = [];
    searchResult.forEach(({ id, folderRef }) => {
        const messageRef = { itemId: id, folderUid: folderRef.uid };
        const conversation = createConversationMetadata(id, folderRef, [messageRef]);
        const conversationRef = { key: conversation.key, id };
        const message = createOnlyMetadata({ internalId: id, folder: folderRef, conversationRef });
        conversations.push(conversation);
        messages.push(message);
    });
    return { conversations, messages };
}

function createConversationMetadata(uid, { key, uid: folderUid }, messages) {
    const sorted = [];
    let lastMessageDate = -1;
    messages.forEach(({ date, itemId, folderUid }) => {
        lastMessageDate = date > lastMessageDate ? date : lastMessageDate;
        const key = messageKey(itemId, folderUid);
        sorted.splice(sortedIndexBy(sorted, key), 0, key);
    });
    return {
        key: uid && key ? messageKey(uid, key) : null,
        folderRef: { key, folderUid },
        remoteRef: { uid },
        status: MessageStatus.IDLE,
        loading: LoadingStatus.NOT_LOADED,
        messages: sorted,
        date: lastMessageDate
    };
}

export function firstMessageInConversationFolder(getters, conversations) {
    return conversations
        .map(conversation =>
            getters
                .CONVERSATION_MESSAGE_BY_KEY(conversation.key)
                .find(m => m.folderRef.key === conversation.folderRef.key)
        )
        .filter(Boolean);
}

export function messagesInConversationFolder(getters, conversations) {
    return conversations.flatMap(conversation =>
        getters
            .CONVERSATION_MESSAGE_BY_KEY(conversation.key)
            .filter(m => m.folderRef.key === conversation.folderRef.key)
    );
}

export function firstMessageFolderKey(conversation) {
    return conversation.messages[0].folderRef.key;
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

export function conversationMustBeRemoved(state, conversation, messages) {
    if (!conversation) {
        return false;
    }
    const keys = new Set(messages.map(message => message.key));
    const folderKey = conversation.folderRef.key;
    return !conversation.messages.some(
        key => !keys.has(key) && state.messages[key] && state.messages[key].folderRef.key === folderKey
    );
}
