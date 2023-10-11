import Vue from "vue";
import {
    ADD_ATTACHMENT,
    ADD_FLAG,
    ADD_MESSAGES,
    DELETE_FLAG,
    MOVE_MESSAGES,
    REMOVE_ATTACHMENT,
    REMOVE_CONVERSATIONS,
    REMOVE_MESSAGE_HEADER,
    REMOVE_MESSAGES,
    RESET_CONVERSATIONS,
    SET_ATTACHMENT_ADDRESS,
    SET_ATTACHMENTS,
    SET_MESSAGE_BCC,
    SET_MESSAGE_CC,
    SET_MESSAGE_COMPOSING,
    SET_MESSAGE_DATE,
    SET_MESSAGE_FROM,
    SET_MESSAGE_HAS_ATTACHMENT,
    SET_MESSAGE_HEADERS,
    SET_MESSAGE_IMAP_UID,
    SET_MESSAGE_INLINE_PARTS_BY_CAPABILITIES,
    SET_MESSAGE_INTERNAL_ID,
    SET_MESSAGE_PREVIEW,
    SET_MESSAGE_STRUCTURE,
    SET_MESSAGE_SUBJECT,
    SET_MESSAGE_TMP_ADDRESSES,
    SET_MESSAGE_TO,
    SET_MESSAGE_LOADING_STATUS,
    SET_MESSAGES_LOADING_STATUS,
    SET_MESSAGE_SIZE,
    SET_MESSAGES_STATUS
} from "~/mutations";

export default {
    [ADD_MESSAGES]: (state, { messages, preserve }) => {
        messages.forEach(message => {
            if (!preserve || !state[message.key]) {
                Vue.set(state, message.key, message);
            }
        });
    },
    [ADD_FLAG]: (state, { messages, flag }) => {
        messages.forEach(({ key }) => state[key].flags.push(flag));
    },
    [DELETE_FLAG]: (state, { messages, flag }) => {
        messages.forEach(({ key }) => (state[key].flags = state[key].flags.filter(f => f !== flag)));
    },
    [REMOVE_MESSAGES]: (state, { messages }) => {
        messages.forEach(({ key }) => {
            Vue.delete(state, key);
        });
    },
    [MOVE_MESSAGES]: (state, { messages }) => messages.forEach(m => (state[m.key].folderRef = m.folderRef)),
    [SET_MESSAGE_PREVIEW]: (state, { key, preview }) => {
        state[key].preview = preview;
    },
    [SET_MESSAGE_SIZE]: (state, { key, size }) => {
        state[key].size = size;
    },
    [SET_MESSAGE_LOADING_STATUS]: (state, { messageKey, status }) => {
        if (state[messageKey]) {
            state[messageKey].loading = status;
        }
    },
    [SET_MESSAGES_STATUS]: (state, messages) => {
        messages.forEach(m => (state[m.key].status = m.status));
    },
    [SET_MESSAGES_LOADING_STATUS]: (state, messages) => {
        messages.forEach(m => (state[m.key].loading = m.loading));
    },
    [SET_MESSAGE_COMPOSING]: (state, { messageKey, composing }) => {
        if (state[messageKey]) {
            state[messageKey].composing = composing;
        }
    },
    [SET_MESSAGE_STRUCTURE]: (state, { messageKey, structure }) => {
        state[messageKey].structure = structure;
    },
    [SET_MESSAGE_SUBJECT]: (state, { messageKey, subject }) => {
        state[messageKey].subject = subject;
    },
    [SET_MESSAGE_DATE]: (state, { messageKey, date }) => {
        state[messageKey].date = date;
    },
    [SET_MESSAGE_FROM]: (state, { messageKey, from }) => {
        state[messageKey].from = from;
    },
    [SET_MESSAGE_HAS_ATTACHMENT]: (state, { key, hasAttachment }) => {
        state[key].hasAttachment = hasAttachment;
    },
    [SET_MESSAGE_HEADERS]: (state, { messageKey, headers }) => {
        state[messageKey].headers = headers;
    },
    [REMOVE_MESSAGE_HEADER]: (state, { messageKey, headerName }) => {
        const index = state[messageKey].headers.findIndex(({ name }) => headerName === name);
        if (index > -1) {
            state[messageKey].headers.splice(index, 1);
        }
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
    [SET_MESSAGE_INLINE_PARTS_BY_CAPABILITIES]: (state, { key, inlinePartsByCapabilities }) => {
        state[key].inlinePartsByCapabilities = inlinePartsByCapabilities;
    },
    [SET_MESSAGE_IMAP_UID]: (state, { key, imapUid }) => {
        state[key].remoteRef.imapUid = imapUid;
    },
    [SET_MESSAGE_TMP_ADDRESSES]: (state, { key, attachments, inlinePartsByCapabilities }) => {
        state[key].attachments = attachments;
        state[key].inlinePartsByCapabilities = inlinePartsByCapabilities;
    },
    [ADD_ATTACHMENT]: (state, { messageKey, attachment }) => {
        state[messageKey].attachments.push(attachment);
    },
    [SET_ATTACHMENTS]: (state, { messageKey, attachments }) => {
        state[messageKey].attachments = attachments;
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

    // Hooks
    [REMOVE_CONVERSATIONS]: (state, conversations) => {
        conversations.forEach(({ messages }) => messages.forEach(key => Vue.delete(state, key)));
    },
    [RESET_CONVERSATIONS]: state => {
        for (const key in state) {
            delete state[key];
        }
    }
};
