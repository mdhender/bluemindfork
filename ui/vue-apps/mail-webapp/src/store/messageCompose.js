import { inject } from "@bluemind/inject";

import mutationTypes from "./mutationTypes";
import { FETCH_SIGNATURE } from "./types/actions";
import { SET_SIGNATURE } from "./types/mutations";

export const mutations = {
    [mutationTypes.SET_DRAFT_EDITOR_CONTENT]: (state, content) => {
        state.messageCompose.editorContent = content;
    },
    [mutationTypes.SET_DRAFT_COLLAPSED_CONTENT]: (state, content) => {
        state.messageCompose.collapsedContent = content;
    },
    [SET_SIGNATURE]: (state, signature) => {
        state.messageCompose.signature = signature;
    }
};

export const actions = {
    async [FETCH_SIGNATURE]({ commit }) {
        const identities = await inject("IUserMailIdentities").getIdentities();
        const defaultIdentity = identities.find(identity => identity.isDefault);
        commit(SET_SIGNATURE, defaultIdentity && defaultIdentity.signature);
    }
};

export const state = {
    messageCompose: {
        editorContent: "",
        collapsedContent: null,
        signature: ""
    }
};
