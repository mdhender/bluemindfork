export const state = {
    folderList: {
        editing: undefined
    }
};

export const mutations = {
    TOGGLE_EDIT_FOLDER: (state, key) => {
        if (state.folderList.editing && state.folderList.editing === key) {
            state.folderList.editing = undefined;
        } else {
            state.folderList.editing = key;
        }
    }
};
