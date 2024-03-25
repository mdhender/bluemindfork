import Vue from "vue";
import { PartsBuilder, MimeType } from "@bluemind/email";

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
    SET_ATTACHMENT_HEADERS,
    SET_MESSAGE_BCC,
    SET_MESSAGE_CC,
    SET_MESSAGE_COMPOSING,
    SET_MESSAGE_DATE,
    SET_MESSAGE_FROM,
    SET_MESSAGE_HEADERS,
    SET_MESSAGE_IMAP_UID,
    SET_MESSAGE_INTERNAL_ID,
    SET_MESSAGE_PREVIEW,
    SET_MESSAGE_STRUCTURE,
    SET_MESSAGE_SUBJECT,
    SET_MESSAGE_TO,
    SET_MESSAGE_LOADING_STATUS,
    SET_MESSAGES_LOADING_STATUS,
    SET_MESSAGE_SIZE,
    SET_MESSAGES_STATUS
} from "~/mutations";
import { cloneDeep } from "lodash";

export default {
    [ADD_MESSAGES]: (state, { messages, preserve }) => {
        messages.forEach(message => {
            if (!preserve || !state[message.key]) {
                Vue.set(state, message.key, message);
            }
        });
    },
    [ADD_FLAG]: (state, { messages, flag }) => {
        messages.forEach(({ key }) => {
            if (state[key]) {
                state[key].flags.push(flag);
            }
        });
    },
    [DELETE_FLAG]: (state, { messages, flag }) => {
        messages.forEach(({ key }) => {
            if (state[key]) {
                state[key].flags = state[key].flags.filter(f => f !== flag);
            }
        });
    },
    [REMOVE_MESSAGES]: (state, { messages }) => {
        messages.forEach(({ key }) => {
            Vue.delete(state, key);
        });
    },
    [MOVE_MESSAGES]: (state, { messages }) => messages.forEach(m => (state[m.key].folderRef = m.folderRef)),
    [SET_MESSAGE_PREVIEW]: (state, { key, preview }) => {
        if (state[key]) {
            state[key].preview = preview;
        }
    },
    [SET_MESSAGE_SIZE]: (state, { key, size }) => {
        if (state[key]) {
            state[key].size = size;
        }
    },
    [SET_MESSAGE_LOADING_STATUS]: (state, { messageKey, status }) => {
        if (state[messageKey]) {
            state[messageKey].loading = status;
        }
    },
    [SET_MESSAGES_STATUS]: (state, messages) => {
        messages.forEach(m => {
            if (state[m.key]) {
                state[m.key].status = m.status;
            }
        });
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
        if (state[messageKey]) {
            state[messageKey].structure = structure;
        }
    },
    [ADD_ATTACHMENT]: (state, { messageKey, attachment }) => {
        let structure = cloneDeep(state[messageKey].structure);
        if (structure.mime === MimeType.MULTIPART_ALTERNATIVE) {
            structure = PartsBuilder.createAttachmentParts([attachment], state[messageKey].structure);
        } else {
            structure.children.push(attachment);
        }
        state[messageKey].structure = structure;
    },
    [SET_MESSAGE_SUBJECT]: (state, { messageKey, subject }) => {
        state[messageKey].subject = subject;
    },
    [SET_MESSAGE_DATE]: (state, { messageKey, date }) => {
        if (state[messageKey]) {
            state[messageKey].date = date;
        }
    },
    [SET_MESSAGE_FROM]: (state, { messageKey, from }) => {
        state[messageKey].from = from;
    },
    [SET_MESSAGE_HEADERS]: (state, { messageKey, headers }) => {
        if (state[messageKey]) {
            state[messageKey].headers = headers;
        }
    },
    [REMOVE_MESSAGE_HEADER]: (state, { messageKey, headerName }) => {
        const index = state[messageKey]?.headers.findIndex(({ name }) => headerName === name);
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
        if (state[key]) {
            state[key].remoteRef.internalId = internalId;
        }
    },
    [SET_MESSAGE_IMAP_UID]: (state, { key, imapUid }) => {
        if (state[key]) {
            state[key].remoteRef.imapUid = imapUid;
        }
    },
    [REMOVE_ATTACHMENT]: (state, { messageKey, address }) => {
        let structure = cloneDeep(state[messageKey].structure);
        const index = structure.children?.findIndex(part => part.address === address);
        if (index > -1) {
            structure.children.splice(index, 1);
        }
        if (structure.children.length === 1 && structure.children[0].mime === MimeType.MULTIPART_ALTERNATIVE) {
            structure = structure.children[0];
        }
        state[messageKey].structure = structure;
    },
    [SET_ATTACHMENT_ADDRESS]: (state, { messageKey, oldAddress, address }) => {
        state[messageKey].structure = updateAttachment(state[messageKey].structure, oldAddress, { address });
    },
    [SET_ATTACHMENT_HEADERS]: (state, { messageKey, address, headers }) => {
        state[messageKey].structure = updateAttachment(state[messageKey].structure, address, { headers });
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

function updateAttachment(structure, address, update = {}) {
    const newStructure = cloneDeep(structure);
    const index = newStructure.children?.findIndex(part => part.address === address);
    if (index > -1) {
        const key = Object.keys(update).pop();
        newStructure.children[index][key] = update[key];
    }
    return newStructure;
}
