import Vue from "vue";
import { RENAME_FOLDER } from "~actions";
import { ADD_FOLDER, ADD_FOLDERS, REMOVE_FOLDER, SET_UNREAD_COUNT, TOGGLE_FOLDER } from "~mutations";

const addFolder = (state, { key, ...folder }) => {
    Vue.set(state, key, { ...folder, key });
};

const addFolders = (state, folders) => {
    folders.forEach(folder => {
        addFolder(state, folder);
    });
};

const renameFolder = (state, { key, name, path }) => {
    state[key].name = name;
    state[key].path = path;
};

const removeFolder = function (state, key) {
    Vue.delete(state, key);
};

//TODO: change parameter name s/count/unread/
const setUnreadCount = (state, { key, count }) => {
    // FIXME: [inconsistency] With `ADD_FOLDER` we can set a negative value for the `unread` property
    if (count >= 0 && state[key].unread !== count) {
        state[key].unread = count;
    }
};

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
