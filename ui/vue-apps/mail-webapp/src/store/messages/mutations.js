import Vue from "vue";
import { MessageStatus } from "../../model/message";
import {
    ADD_ATTACHMENT,
    ADD_FLAG,
    ADD_MESSAGES,
    DELETE_FLAG,
    REMOVE_ATTACHMENT,
    REMOVE_MESSAGES,
    MOVE_MESSAGES,
    SET_ATTACHMENT_ADDRESS,
    SET_ATTACHMENT_ENCODING,
    SET_ATTACHMENT_PROGRESS,
    SET_ATTACHMENT_STATUS,
    SET_MESSAGES_STATUS,
    SET_MESSAGE_BCC,
    SET_MESSAGE_CC,
    SET_MESSAGE_COMPOSING,
    SET_MESSAGE_DATE,
    SET_MESSAGE_HAS_ATTACHMENT,
    SET_MESSAGE_HEADERS,
    SET_MESSAGE_INTERNAL_ID,
    SET_MESSAGE_IMAP_UID,
    SET_MESSAGE_PREVIEW,
    SET_MESSAGE_LIST,
    SET_MESSAGE_SUBJECT,
    SET_MESSAGE_TO,
    SET_UNREAD_COUNT
} from "~mutations";
import { Flag } from "@bluemind/email";

export default {
    [ADD_MESSAGES]: (state, messages) => {
        messages.forEach(message => {
            Vue.set(state, message.key, message);
        });
    },
    [ADD_FLAG]: (state, { messages, flag }) => {
        messages.forEach(({ key }) => state[key].flags.push(flag));
    },
    [DELETE_FLAG]: (state, { messages, flag }) => {
        messages.forEach(({ key }) => (state[key].flags = state[key].flags.filter(f => f !== flag)));
    },
    [REMOVE_MESSAGES]: removeMessages,
    [MOVE_MESSAGES]: (state, { messages }) => removeMessages(state, messages),
    [SET_UNREAD_COUNT]: (state, { key, unread }) => {
        if (unread === 0) {
            //FIXME: This cannot be rolled back...
            Object.values(state).forEach(({ status, folderRef, flags }) => {
                if (status === MessageStatus.LOADED && folderRef.key === key && !flags.includes(Flag.SEEN)) {
                    flags.push(Flag.SEEN);
                }
            });
        }
    },
    [SET_MESSAGE_PREVIEW]: (state, { key, preview }) => {
        state[key].preview = preview;
    },
    [SET_MESSAGES_STATUS]: (state, messages) => {
        messages.forEach(m => (state[m.key].status = m.status));
    },
    [SET_MESSAGE_LIST]: (state, messages) => {
        messages.filter(message => !state[message.key]).forEach(message => Vue.set(state, message.key, message));
    },
    [SET_MESSAGE_COMPOSING]: (state, { messageKey, composing }) => {
        state[messageKey].composing = composing;
    },
    [SET_MESSAGE_SUBJECT]: (state, { messageKey, subject }) => {
        state[messageKey].subject = subject;
    },
    [SET_MESSAGE_DATE]: (state, { messageKey, date }) => {
        state[messageKey].date = date;
    },
    [SET_MESSAGE_HAS_ATTACHMENT]: (state, { key, hasAttachment }) => {
        state[key].hasAttachment = hasAttachment;
    },
    [SET_MESSAGE_HEADERS]: (state, { messageKey, headers }) => {
        state[messageKey].headers = headers;
    },
    [SET_MESSAGE_TO]: (state, { messageKey, to }) => {
        state[messageKey].to = to;
    },
    [SET_MESSAGE_CC]: (state, { messageKey, cc }) => {
        state[messageKey].cc = cc;
    },
    [SET_MESSAGE_BCC]: (state, { messageKey, bcc }) => {
        state[messageKey].bcc = bcc;
    },
    [SET_MESSAGE_INTERNAL_ID]: (state, { key, internalId }) => {
        state[key].remoteRef.internalId = internalId;
    },
    [SET_MESSAGE_IMAP_UID]: (state, { key, imapUid }) => {
        state[key].remoteRef.imapUid = imapUid;
    },
    [ADD_ATTACHMENT]: (state, { messageKey, attachment }) => {
        state[messageKey].attachments.push(attachment);
    },
    [REMOVE_ATTACHMENT]: (state, { messageKey, address }) => {
        const attachments = state[messageKey].attachments;
        const index = attachments.findIndex(a => a.address === address);
        attachments.splice(index, 1);
    },
    [SET_ATTACHMENT_ADDRESS]: (state, { messageKey, oldAddress, address }) => {
        const attachment = state[messageKey].attachments.find(a => a.address === oldAddress);
        if (attachment) {
            attachment.address = address;
        }
    },
    [SET_ATTACHMENT_STATUS]: (state, { messageKey, address, status }) => {
        const attachment = state[messageKey].attachments.find(a => a.address === address);
        attachment.status = status;
    },
    [SET_ATTACHMENT_ENCODING]: (state, { messageKey, address, charset, encoding }) => {
        const attachment = state[messageKey].attachments.find(a => a.address === address);
        attachment.charset = charset;
        attachment.encoding = encoding;
    },
    [SET_ATTACHMENT_PROGRESS]: (state, { messageKey, address, loaded, total }) => {
        const attachment = state[messageKey].attachments.find(a => a.address === address);
        attachment.progress = { loaded, total };
    }
};
function removeMessages(state, messages) {
    messages.forEach(({ key }) => {
        Vue.delete(state, key);
    });
}
