import merge from "lodash.merge";
import cloneDeep from "lodash.clonedeep";
import pick from "lodash.pick";

import ItemUri from "@bluemind/item-uri";

export function createOnlyMetadata({ internalId, folder: { key, uid } }) {
    return {
        key: internalId && key ? ItemUri.encode(internalId, key) : null,
        folderRef: { key, uid },
        remoteRef: { internalId },
        status: MessageStatus.NOT_LOADED
    };
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

        partContentByMimeType: {}, // REMOVE ME and use byAddress
        partContentByAddress: {},

        attachments: [],
        inlinePartsByCapabilities: []
    };
    return merge(createOnlyMetadata({ folder: {} }), emptyData);
}

export function partialCopy(message, properties = []) {
    return cloneDeep(pick(message, properties.concat("key", "folderRef", "status", "remoteRef")));
}

export const MessageStatus = {
    NOT_LOADED: "NOT-LOADED",
    PENDING: "PENDING",
    LOADED: "LOADED",
    REMOVED: "REMOVED",
    SAVING: "SAVING",
    SAVE_ERROR: "SAVE_ERROR",
    SENDING: "SENDING"
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

export async function clean(partAddresses, newAttachments, service) {
    const promises = [];
    Object.keys(partAddresses).forEach(mimeType => {
        partAddresses[mimeType].forEach(address => {
            promises.push(service.removePart(address));
        });
    });
    newAttachments.forEach(attachment => promises.push(service.removePart(attachment.address)));
    return Promise.all(promises);
}

export function updateKey(message, internalId, folder) {
    const newKey = ItemUri.encode(internalId, folder.key);
    const newMessage = { ...message };
    newMessage.key = newKey;
    newMessage.remoteRef.internalId = internalId;
    newMessage.folderRef = { key: folder.key, uid: folder.remoteRef.uid };
    return newMessage;
}
