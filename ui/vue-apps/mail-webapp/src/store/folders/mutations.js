import Vue from "vue";

export const ADD_FOLDER = "ADD_FOLDER";
const addFolder = (state, { key, ...folder }) => {
    Vue.set(state, key, { ...folder, key });
};

export const ADD_FOLDERS = "ADD_FOLDERS";
const addFolders = (state, folders) => {
    folders.forEach(folder => {
        addFolder(state, folder);
    });
};

export const RENAME_FOLDER = "RENAME_FOLDER";
const renameFolder = (state, { key, name, path }) => {
    state[key].name = name;
    state[key].path = path;
};

//TODO: change signature to: ({key}) => {}
export const REMOVE_FOLDER = "REMOVE_FOLDER";
const removeFolder = function (state, key) {
    Vue.delete(state, key);
};

//TODO: change parameter name s/count/unread/
export const SET_UNREAD_COUNT = "SET_UNREAD_COUNT";
const setUnreadCount = (state, { key, count }) => {
    // FIXME: [inconsistency] With `ADD_FOLDER` we can set a negative value for the `unread` property
    if (count >= 0 && state[key].unread !== count) {
        state[key].unread = count;
    }
};

//TODO: change signature to: ({key}) => {}
export const TOGGLE_FOLDER = "TOGGLE_FOLDER";
const toggleFolder = (state, key) => {
    state[key].expanded = !state[key].expanded;
};

export default {
    [ADD_FOLDER]: addFolder,
    [ADD_FOLDERS]: addFolders,
    [RENAME_FOLDER]: renameFolder,
    [REMOVE_FOLDER]: removeFolder,
    [SET_UNREAD_COUNT]: setUnreadCount,
    [TOGGLE_FOLDER]: toggleFolder
};
