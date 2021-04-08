import merge from "lodash.merge";
import cloneDeep from "lodash.clonedeep";
import pick from "lodash.pick";

import { LoadingStatus } from "./loading-status";

export function createOnlyMetadata({ internalId, folder: { key, uid } }) {
    return {
        key: internalId >= 0 && key ? messageKey(internalId, key) : null,
        folderRef: { key, uid },
        remoteRef: { internalId },
        status: MessageStatus.IDLE,
        loading: LoadingStatus.NOT_LOADED
    };
}
export function messageKey(id, folderKey) {
    const string = folderKey + "/" + id;
    let hash = 0;
    for (let i = 0; i < string.length; i++) {
        hash = ((hash << 5) - hash + string.charCodeAt(i)) & 0xffffffff;
    }

    return hash;
}

export function createWithMetadata(metadata) {
    const messageMetadata = createOnlyMetadata(metadata);
    const emptyMessage = create();
    return merge(emptyMessage, messageMetadata);
}

export function create() {
    const emptyData = {
        remoteRef: { imapUid: null },
        flags: [],
        date: null,
        headers: [],
        subject: "",
        preview: "",
        composing: false,
        version: null,

        // sender & recipients
        from: {
            address: "",
            dn: ""
        },
        to: [],
        cc: [],
        bcc: [],

        // used only by reply / forward
        messageId: "",
        references: [],

        // parts
        hasAttachment: false,
        hasICS: false,

        eventInfo: {
            isCounterEvent: false,
            eventUid: "",
            icsUid: "",
            needsReply: false
        },

        attachments: [],
        inlinePartsByCapabilities: []
    };
    return merge(createOnlyMetadata({ folder: {} }), emptyData);
}

export function partialCopy(message, properties = []) {
    return cloneDeep(pick(message, properties.concat("key", "folderRef", "status", "remoteRef")));
}

export const MessageStatus = {
    IDLE: "IDLE",
    REMOVED: "REMOVED",
    SAVING: "SAVING",
    SAVE_ERROR: "SAVE_ERROR",
    SENDING: "SENDING",
    SENT: "SENT"
};

export const MessageCreationModes = {
    NEW: "NEW",
    REPLY: "REPLY",
    REPLY_ALL: "REPLY-ALL",
    FORWARD: "FORWARD"
};

export const MessageHeader = {
    MAIL_FOLLOWUP_TO: "Mail-Followup-To",
    MAIL_REPLY_TO: "Mail-Reply-To",
    REPLY_TO: "Reply-To",
    IN_REPLY_TO: "In-Reply-To",

    X_BM_DRAFT_INFO: "X-Bm-Draft-Info",
    X_BM_DRAFT_REFRESH_DATE: "X-Bm-Draft-Refresh-Date",
    X_BM_EVENT: "X-BM-Event",
    X_BM_EVENT_COUNTERED: "X-BM-Event-Countered"
};

export const MessageReplyAttributeSeparator = "data-bm-reply-separator";
export const MessageForwardAttributeSeparator = "data-bm-forward-separator";

export function equal(a, b) {
    return (
        a &&
        b &&
        (a.key === b.key ||
            (a.remoteRef &&
                a.remoteRef.internalId &&
                a.folderRef &&
                a.remoteRef.internalId === b.remoteRef?.internalId &&
                a.folderRef.key === b.folderRef?.key))
    );
}
