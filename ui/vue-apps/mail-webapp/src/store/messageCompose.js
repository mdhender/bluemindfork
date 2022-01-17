import { inject } from "@bluemind/inject";
import { LOAD_MAX_MESSAGE_SIZE } from "~/actions";
import {
    RESET_PENDING_ATTACHMENTS,
    SET_DRAFT_COLLAPSED_CONTENT,
    SET_DRAFT_EDITOR_CONTENT,
    SET_MAX_MESSAGE_SIZE,
    SET_PENDING_ATTACHMENTS,
    SET_SAVED_INLINE_IMAGES,
    SHOW_SENDER
} from "~/mutations";
import { IS_SENDER_SHOWN } from "~/getters";
import templateChooser from "./templateChooser";

export default {
    mutations: {
        [RESET_PENDING_ATTACHMENTS]: state => {
            state.pendingAttachments = [];
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
        [SET_PENDING_ATTACHMENTS]: (state, attachments) => {
            state.pendingAttachments = attachments;
        },
        [SET_MAX_MESSAGE_SIZE](state, size) {
            state.maxMessageSize = size;
        },
        [SHOW_SENDER]: (state, value) => (state.isSenderShown = value)
    },

    actions: {
        async [LOAD_MAX_MESSAGE_SIZE]({ commit }, userId) {
            const { messageMaxSize } = await inject("MailboxesPersistence").getMailboxConfig(userId);
            // take into account the email base64 encoding : 33% more space
            commit(SET_MAX_MESSAGE_SIZE, messageMaxSize / 1.33);
        }
    },

    getters: {
        [IS_SENDER_SHOWN]: state => userSettings => state.isSenderShown || !!userSettings.always_show_from
    },

    state: {
        editorContent: "",
        collapsedContent: null,
        inlineImagesSaved: [],
        maxMessageSize: 0,
        pendingAttachments: [], // used only to store forwarded attachments when they are not uploaded
        isSenderShown: false
    },
    modules: {
        templateChooser
    }
};
