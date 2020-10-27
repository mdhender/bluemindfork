import merge from "lodash.merge";
import cloneDeep from "lodash.clonedeep";
import pick from "lodash.pick";

import ItemUri from "@bluemind/item-uri";
import { MimeType } from "@bluemind/email";

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

        partContentByMimeType: {}, // REMOVE ME and use byAddress
        partContentByAddress: {},

        attachments: [],
        inlinePartsByCapabilities: [],
        multipartAddresses: {}
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
    X_BM_EVENT: "X-BM-Event"
};

export const MessageReplyAttributeSeparator = "data-bm-reply-separator";
export const MessageForwardAttributeSeparator = "data-bm-forward-separator";

export function isEmpty(message, content) {
    return (
        message.to.length === 0 &&
        message.cc.length === 0 &&
        message.bcc.length === 0 &&
        isSubjectEmpty(message.subject) &&
        isEmptyContent(content)
    );
}

function isEmptyContent(content) {
    const consideredAsEmptyRegex = /^<div id="bm-composer-content-wrapper"><style><\/style><\/div>$/;
    return !content || consideredAsEmptyRegex.test(content) || content === "";
}

function isSubjectEmpty(subject) {
    return subject === "" || subject === " ";
}

// FIXME: move it like a message action because it must store parts in state ?
export async function fetch(messageImapUid, service, part, isAttachment) {
    const stream = await service.fetch(messageImapUid, part.address, part.encoding, part.mime, part.charset);
    if (!isAttachment && (MimeType.isText(part) || MimeType.isHtml(part) || MimeType.isCalendar(part))) {
        return new Promise(resolve => {
            const reader = new FileReader();
            reader.readAsText(stream, part.charset);
            reader.addEventListener("loadend", e => {
                resolve(e.target.result);
            });
        });
    } else {
        return stream;
    }
}

export async function fetchAll(messageImapUid, service, parts, isAttachment) {
    return Promise.all(parts.map(part => fetch(messageImapUid, service, part, isAttachment)));
}

export async function clean(partAddresses, attachmentAddresses, service) {
    const promises = [];
    Object.keys(partAddresses).forEach(mimeType => {
        partAddresses[mimeType].forEach(address => {
            promises.push(service.removePart(address));
        });
    });
    attachmentAddresses.forEach(address => promises.push(service.removePart(address)));
    return Promise.all(promises);
}
