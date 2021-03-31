import { inject } from "@bluemind/inject";

import { LOAD_MAX_MESSAGE_SIZE } from "~actions";
import {
    RESET_ATTACHMENTS_FORWARDED,
    SET_ATTACHMENTS_FORWARDED,
    SET_DRAFT_COLLAPSED_CONTENT,
    SET_DRAFT_EDITOR_CONTENT,
    SET_MAX_MESSAGE_SIZE,
    SET_SAVED_INLINE_IMAGES
} from "~mutations";

export default {
    mutations: {
        [RESET_ATTACHMENTS_FORWARDED]: state => {
            state.forwardedAttachments = [];
        },
        [SET_DRAFT_EDITOR_CONTENT]: (state, content) => {
            state.editorContent = content;
        },
        [SET_DRAFT_COLLAPSED_CONTENT]: (state, collapsed) => {
            state.collapsedContent = collapsed;
        },
        [SET_SAVED_INLINE_IMAGES]: (state, inlineImages) => {
            state.inlineImagesSaved = inlineImages;
        },
        [SET_ATTACHMENTS_FORWARDED]: (state, forwardedAttachments) => {
            state.forwardedAttachments = forwardedAttachments;
        },
        [SET_MAX_MESSAGE_SIZE](state, size) {
            state.maxMessageSize = size;
        }
    },

    actions: {
        async [LOAD_MAX_MESSAGE_SIZE]({ commit }, userId) {
            const { messageMaxSize } = await inject("MailboxesPersistence").getMailboxConfig(userId);
            // take into account the email base64 encoding : 33% more space
            commit(SET_MAX_MESSAGE_SIZE, messageMaxSize / 1.33);
        }
    },

    state: {
        editorContent: "",
        collapsedContent: null,
        inlineImagesSaved: [],
        maxMessageSize: 0,
        forwardedAttachments: [] // used only to store forwarded attachments when they are not uploaded
    }
};
