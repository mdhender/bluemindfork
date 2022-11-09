import cloneDeep from "lodash.clonedeep";
import escapeRegExp from "lodash.escaperegexp";
import merge from "lodash.merge";
import pick from "lodash.pick";

import { LoadingStatus } from "../loading-status";
import EmlParser from "./EmlParser";
import MessageAdaptor from "./MessageAdaptor";
import { Flag } from "@bluemind/email";

export function createOnlyMetadata({ internalId, folder: { key, uid }, conversationRef, date }) {
    return {
        key: internalId >= 0 && key ? messageKey(internalId, key) : null,
        folderRef: { key, uid },
        remoteRef: { internalId },
        conversationRef,
        status: MessageStatus.IDLE,
        loading: LoadingStatus.NOT_LOADED,
        date
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
        size: 0,
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
        conversationId: undefined,

        // parts
        hasAttachment: false,
        hasICS: false,

        eventInfo: {
            isCounterEvent: false,
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
    NEW: "NEW",
    SAVING: "SAVING",
    SAVE_ERROR: "SAVE_ERROR",
    SENDING: "SENDING",
    SENT: "SENT"
};

export const MessageCreationModes = {
    NEW: "NEW",
    REPLY: "REPLY",
    REPLY_ALL: "REPLY-ALL",
    FORWARD: "FORWARD",
    EDIT_AS_NEW: "EDIT_AS_NEW",
    EDIT: "EDIT",
    FORWARD_AS_EML: "FORWARD_AS_EML"
};

export const MessageHeader = {
    MAIL_FOLLOWUP_TO: "Mail-Followup-To",
    MAIL_REPLY_TO: "Mail-Reply-To",
    MESSAGE_ID: "Message-ID",
    REPLY_TO: "Reply-To",
    IN_REPLY_TO: "In-Reply-To",
    REFERENCES: "References",
    DELIVERED_TO: "Delivered-To",

    X_LOOP: "X-Loop",
    X_ORIGINAL_TO: "X-Original-To",

    X_BM_DRAFT_INFO: "X-Bm-Draft-Info",
    X_BM_DRAFT_REFRESH_DATE: "X-Bm-Draft-Refresh-Date",
    X_BM_EVENT_COUNTERED: "X-BM-Event-Countered",
    X_BM_EVENT: "X-BM-Event",
    X_BM_RESOURCEBOOKING: "X-BM-ResourceBooking",
    X_BM_REWRITE: "X-BM-Rewrite",
    X_BM_SENT_FOLDER: "X-BM-Sent-Folder"
};

export const MessageReplyAttributeSeparator = "data-bm-reply-separator";
export const MessageForwardAttributeSeparator = "data-bm-forward-separator";
export const MessageQuoteMozillaClass = "moz-cite-prefix";
export const MessageQuoteGmailClass = "gmail_quote";
export const MessageQuoteProtonClass = "protonmail_quote";
export const MessageQuoteYahooClass = "yahoo_quoted";
export const MessageQuoteClasses = [
    MessageReplyAttributeSeparator,
    MessageForwardAttributeSeparator,
    MessageQuoteMozillaClass,
    MessageQuoteGmailClass,
    MessageQuoteProtonClass,
    MessageQuoteYahooClass
];
export const MessageQuoteOutlookId = "divRplyFwdMsg";

export function isUnread(message) {
    return message.loading === LoadingStatus.LOADED && !message.flags.includes(Flag.SEEN);
}

export function isFlagged(message) {
    return message.loading === LoadingStatus.LOADED && message.flags.includes(Flag.FLAGGED);
}

/** Extract multi-valued / whitespace separated values from given header. */
export function extractHeaderValues(message, headerName) {
    const header = message.headers.find(h => h.name.toUpperCase() === headerName.toUpperCase());
    return header && header.values && header.values.length
        ? header.values.reduce((a, b) => (a.length ? a + " " + b : b), "").split(/\s+/)
        : undefined;
}

/**
 * Try to detect a message is a Forward.
 * @see https://stackoverflow.com/questions/4735293/forwarded-email-detection
 */
export function isForward(message) {
    let hasAForwardFriendlyHeader = false;
    let hasBmDraftHeaderWithFwdType = false;

    message.headers?.forEach(header => {
        // MUAs should add one of these headers
        if (
            [
                MessageHeader.MAIL_FOLLOWUP_TO.toUpperCase(),
                MessageHeader.DELIVERED_TO.toUpperCase(),
                MessageHeader.X_LOOP.toUpperCase(),
                MessageHeader.X_ORIGINAL_TO.toUpperCase()
            ].includes(header.name.toUpperCase())
        ) {
            hasAForwardFriendlyHeader = true;
        }

        // BM specific
        if (
            header.name.toUpperCase() === MessageHeader.X_BM_DRAFT_INFO.toUpperCase() &&
            header.values?.some(v => JSON.parse(v).type === MessageCreationModes.FORWARD)
        ) {
            hasBmDraftHeaderWithFwdType = true;
        }
    });

    // MUAs tend to add the 'Fwd' token in the subject (which may be rewritten by the user)
    const subjectStartsWithFwd = /^\[*Fwd?:/i.test(message.subject);
    const subjectEndsWithFwd = /\(fwd?\)$/i.test(message.subject);
    const hasAForwardFriendlySubject = subjectStartsWithFwd || subjectEndsWithFwd;

    const shouldBeForward = hasBmDraftHeaderWithFwdType || hasAForwardFriendlyHeader || hasAForwardFriendlySubject;

    return shouldBeForward;
}

/**
 * Create a portable name for the EML file of the given message.
 * @see  https://www.mtu.edu/umc/services/websites/writing/characters-avoid/
 * @param {Object} message the message used to generate an EML file name
 * @param {String} fallBackName a fall-back name if the use of message has failed (ex: empty subject)
 * @returns a portable EML file name
 */
export function createEmlName(message, fallBackName) {
    const maxChars = 31;
    const extension = ".eml";
    const illegalChars = "#<>$+%!`&*'|{}?\"=/\\:@";
    const illegalCharsRegex = new RegExp(`[${escapeRegExp(illegalChars)}\\s]+`, "g");
    let subject = message.subject || fallBackName;
    subject = subject.trim();
    const replacementChar = "-";
    subject = subject.replaceAll(illegalCharsRegex, replacementChar);
    if (subject.length > maxChars - extension.length) {
        subject = subject.substring(0, maxChars - extension.length);
    }
    return `${subject}${extension}`;
}

export default {
    create,
    createEmlName,
    createOnlyMetadata,
    createWithMetadata,
    extractHeaderValues,
    isFlagged,
    isForward,
    isUnread,
    EmlParser,
    MessageAdaptor,
    MessageCreationModes,
    MessageForwardAttributeSeparator,
    MessageHeader,
    messageKey,
    MessageQuoteClasses,
    MessageQuoteGmailClass,
    MessageQuoteMozillaClass,
    MessageQuoteOutlookId,
    MessageQuoteProtonClass,
    MessageQuoteYahooClass,
    MessageReplyAttributeSeparator,
    MessageStatus,
    partialCopy
};
