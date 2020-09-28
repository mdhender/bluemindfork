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
    const consideredAsEmptyRegex = /^<div>(<br>)*<\/div>$/;
    return !content || consideredAsEmptyRegex.test(content) || content === "";
}

function isSubjectEmpty(subject) {
    return subject === "" || subject === " ";
}
