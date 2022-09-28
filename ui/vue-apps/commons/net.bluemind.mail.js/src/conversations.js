import { MessageCreationModes, messageKey } from "./message";
import { LoadingStatus } from "./loading-status";
import { draftInfoHeader } from "./draft";
import { isDraftFolder } from "./folder";

export function createConversationStub(id, folderRef) {
    return {
        folderRef,
        key: messageKey(id, folderRef.key),
        loading: LoadingStatus.NOT_LOADED,
        messages: [],
        remoteRef: { uid: id }
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
        MessageCreationModes.FORWARD,
        MessageCreationModes.FORWARD_AS_EML
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

export function idToUid(value) {
    const bytes = 64n;
    // eslint-disable-next-line no-undef
    const bigIntValue = BigInt(value);
    if (bigIntValue < 0n) {
        return (bigIntValue + (1n << bytes)).toString(16);
    }
    return bigIntValue.toString(16);
}

export default {
    conversationMustBeRemoved,
    createConversationStub,
    firstMessageFolderKey,
    firstMessageInConversationFolder,
    idToUid,
    messagesInConversationFolder,
    sortConversationMessages
};
