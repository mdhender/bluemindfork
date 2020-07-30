export const TOGGLE_EDIT_FOLDER = "TOGGLE_EDIT_FOLDER";

export const state = {
    folderList: {
        editing: undefined
    }
};

export const mutations = {
    [TOGGLE_EDIT_FOLDER]: (state, key) => {
        console.log("COUCOUUUUU TOGGLE EDIT FOLDER !!!!!!!!", key);
        if (state.folderList.editing && state.folderList.editing === key) {
            state.folderList.editing = undefined;
        } else {
            state.folderList.editing = key;
        }
    }
};
