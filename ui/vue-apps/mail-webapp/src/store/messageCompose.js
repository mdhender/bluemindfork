import { inject } from "@bluemind/inject";

import { FETCH_SIGNATURE } from "~actions";
import {
    SET_DRAFT_COLLAPSED_CONTENT,
    SET_DRAFT_EDITOR_CONTENT,
    SET_SAVED_INLINE_IMAGES,
    SET_SIGNATURE
} from "~mutations";

export default {
    mutations: {
        [SET_DRAFT_EDITOR_CONTENT]: (state, content) => {
            state.editorContent = content;
        },
        [SET_DRAFT_COLLAPSED_CONTENT]: (state, collapsed) => {
            state.collapsedContent = collapsed;
        },
        [SET_SAVED_INLINE_IMAGES]: (state, inlineImages) => {
            state.inlineImagesSaved = inlineImages;
        },
        [SET_SIGNATURE]: (state, signature) => {
            state.signature = signature;
        }
    },

    actions: {
        async [FETCH_SIGNATURE]({ commit }) {
            const identities = await inject("IUserMailIdentities").getIdentities();
            const defaultIdentity = identities.find(identity => identity.isDefault);
            commit(SET_SIGNATURE, defaultIdentity && defaultIdentity.signature);
        }
    },

    state: {
        editorContent: "",
        collapsedContent: null,
        inlineImagesSaved: [],
        signature: ""
    }
};
