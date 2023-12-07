import { Flag } from "@bluemind/email";
import { isDeleted, isFlagged, isUnread, MessageCreationModes, messageKey } from "./message";
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

function buildConversationMetadata(key, conversation, messages) {
    const messageMetadata = mergeMessagesMetadata(conversation.folderRef.key, messages);
    return {
        ...messageMetadata,
        key,
        remoteRef: conversation.remoteRef,
        folderRef: conversation.folderRef,
        messages: messages.map(m => m.key),
        senders: messages
            .reverse()
            .reduce(
                (results, message) =>
                    !message.from?.address || results.some(({ address }) => address === message.from.address)
                        ? results
                        : [...results, message.from],
                []
            )
    };
}

function mergeMessagesMetadata(folderKey, messages) {
    let subject = messages[0]?.subject,
        from = messages[0]?.from,
        to = messages[0]?.to,
        cc = messages[0]?.cc,
        bcc = messages[0]?.bcc,
        unreadCount = 0,
        flags = messages.length > 0 ? new Set([Flag.SEEN, Flag.DELETED]) : new Set(),
        loading = messages.length > 0 ? LoadingStatus.LOADING : LoadingStatus.ERROR,
        hasAttachment = false,
        hasICS = false,
        preview,
        date = -1,
        size = 0,
        headers = [];
    messages.forEach(m => {
        if (m.folderRef.key === folderKey) {
            if (isUnread(m)) {
                unreadCount++;
                flags.delete(Flag.SEEN);
            }
            if (!isDeleted(m)) {
                flags.delete(Flag.DELETED);
            }
            if (isFlagged(m)) {
                flags.add(Flag.FLAGGED);
            }
            size = m.size > size ? m.size : size;
        }

        m.flags?.forEach(flag => [Flag.ANSWERED, Flag.FORWARDED].includes(flag) && flags.add(flag));

        if (m.composing || (m.loading === LoadingStatus.LOADED && m.folderRef.key === folderKey)) {
            loading = LoadingStatus.LOADED;
        }
        if (m.hasAttachment) {
            hasAttachment = true;
        }
        if (m.hasICS) {
            hasICS = true;
        }
        if (m.folderRef.key === folderKey && m.date > date) {
            preview = m.preview;
            date = m.date;
        }
        if (m.headers) {
            m.headers.forEach(header => {
                if (!isHeaderBlacklisted(header)) {
                    headers.push(header);
                }
            });
        }
    });

    return {
        subject,
        from,
        to,
        cc,
        bcc,
        unreadCount,
        flags: Array.from(flags),
        loading,
        hasAttachment,
        hasICS,
        preview,
        date,
        size,
        headers
    };
}

function isHeaderBlacklisted({ name }) {
    const lowered = name.toLowerCase();
    return lowered.startsWith("x-bm-") || name === "references" || name === "in-reply-to";
}

export default {
    buildConversationMetadata,
    conversationMustBeRemoved,
    createConversationStub,
    firstMessageFolderKey,
    firstMessageInConversationFolder,
    idToUid,
    messagesInConversationFolder,
    sortConversationMessages
};
