import mutationTypes from "./mutationTypes";

export const mutations = {
    [mutationTypes.SET_DRAFT_EDITOR_CONTENT]: (state, content) => {
        state.messageCompose.editorContent = content;
    },
    [mutationTypes.SET_DRAFT_COLLAPSED_CONTENT]: (state, content) => {
        state.messageCompose.collapsedContent = content;
    }
};

export const state = {
    messageCompose: {
        editorContent: "",
        collapsedContent: null
    }
};
