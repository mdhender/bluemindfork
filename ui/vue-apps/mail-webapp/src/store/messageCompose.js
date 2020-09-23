import mutationTypes from "./mutationTypes";

export const mutations = {
    [mutationTypes.SET_DRAFT_EDITOR_CONTENT]: (state, content) => {
        state.messageCompose.editorContent = content;
    }
};

export const state = {
    messageCompose: {
        editorContent: ""
    }
};
