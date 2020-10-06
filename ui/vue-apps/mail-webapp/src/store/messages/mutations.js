import Vue from "vue";
import mutationTypes from "../mutationTypes";
import { MessageStatus } from "../../model/message";

export default {
    [mutationTypes.ADD_MESSAGES]: (state, messages) => {
        messages.forEach(message => {
            if (state[message.key]) {
                message.composing = state[message.key].composing;
            }
            Vue.set(state, message.key, message);
        });
    },
    [mutationTypes.ADD_FLAG]: (state, { keys, flag }) => {
        keys.forEach(key => {
            if (state[key].status === MessageStatus.LOADED && !state[key].flags.includes(flag)) {
                state[key].flags.push(flag);
            }
        });
    },
    [mutationTypes.DELETE_FLAG]: (state, { keys, flag }) => {
        keys.forEach(key => {
            if (state[key].status === MessageStatus.LOADED && state[key].flags.includes(flag)) {
                state[key].flags = state[key].flags.filter(f => f !== flag);
            }
        });
    },
    [mutationTypes.REMOVE_MESSAGES]: (state, keys) => {
        keys.forEach(key => {
            if (state[key] && state[key].attachments) {
                state[key].attachments
                    .filter(attachment => attachment.contentUrl)
                    .forEach(attachment => {
                        URL.revokeObjectURL(attachment.contentUrl);
                    });
            }
            Vue.delete(state, key);
        });
    },
    [mutationTypes.SET_MESSAGES_STATUS]: (state, messages) => {
        messages.forEach(m => (state[m.key].status = m.status));
    },
    [mutationTypes.SET_MESSAGE_LIST]: (state, messages) => {
        messages.filter(message => !state[message.key]).forEach(message => Vue.set(state, message.key, message));
    },
    [mutationTypes.SET_MESSAGE_COMPOSING]: (state, { messageKey, composing }) => {
        state[messageKey].composing = composing;
    },
    [mutationTypes.SET_MESSAGE_SUBJECT]: (state, { messageKey, subject }) => {
        state[messageKey].subject = subject;
    },
    [mutationTypes.SET_MESSAGE_RECIPIENTS]: (state, { messageKey, recipients }) => {
        state[messageKey].to = recipients.to;
        state[messageKey].cc = recipients.cc;
        state[messageKey].bcc = recipients.bcc;
    },
    [mutationTypes.SET_MESSAGE_DATE]: (state, { messageKey, date }) => {
        state[messageKey].date = date;
    },
    [mutationTypes.SET_MESSAGE_HEADERS]: (state, { messageKey, headers }) => {
        state[messageKey].headers = headers;
    },
    [mutationTypes.SET_MESSAGE_ATTACHMENTS]: (state, { messageKey, attachments }) => {
        state[messageKey].attachments = attachments;
    },
    [mutationTypes.ADD_ATTACHMENT]: (state, { messageKey, attachment }) => {
        state[messageKey].attachments.push(attachment);
    },
    [mutationTypes.REMOVE_ATTACHMENT]: (state, { messageKey, address }) => {
        const attachments = state[messageKey].attachments;
        const index = attachments.findIndex(a => a.address === address);
        if (attachments[index].contentUrl) {
            URL.revokeObjectURL(attachments[index].contentUrl);
        }
        attachments.splice(index, 1);
    },
    [mutationTypes.UPDATE_ATTACHMENT]: (state, { messageKey, oldAddress, address, contentUrl }) => {
        const attachment = state[messageKey].attachments.find(a => a.address === oldAddress);
        attachment.contentUrl = contentUrl;
        attachment.address = address;
    },
    [mutationTypes.SET_ATTACHMENT_STATUS]: (state, { messageKey, address, status }) => {
        const attachment = state[messageKey].attachments.find(a => a.address === address);
        attachment.status = status;
    },
    [mutationTypes.SET_ATTACHMENT_PROGRESS]: (state, { messageKey, address, loaded, total }) => {
        const attachment = state[messageKey].attachments.find(a => a.address === address);
        attachment.progress = { loaded, total };
    },
    [mutationTypes.SET_ATTACHMENT_CONTENT_URL]: (state, { messageKey, address, url }) => {
        if (state[messageKey]) {
            const attachment = state[messageKey].attachments.find(a => a.address === address);
            attachment.contentUrl = url;
        }
    }
};
