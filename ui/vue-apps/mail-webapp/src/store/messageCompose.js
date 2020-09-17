import mutationTypes from "./mutationTypes";

export const mutations = {
    [mutationTypes.SET_DRAFT_EDITOR_CONTENT]: (state, content) => (state.messageCompose.editorContent = content)
};

export const state = {
    /**
     * Yet we can only have one composer at a time.
     * To allow user to open multiple composers then we must have here an array of messageCompose
     */
    messageCompose: { editorContent: "" }
};
