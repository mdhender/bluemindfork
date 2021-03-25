import { Flag } from "@bluemind/email";
import Vue from "vue";
import { RENAME_FOLDER } from "~actions";
import {
    ADD_FLAG,
    ADD_FOLDER,
    DELETE_FLAG,
    MOVE_MESSAGES,
    REMOVE_FOLDER,
    REMOVE_MESSAGES,
    SET_ACTIVE_FOLDER,
    SET_FOLDER_EXPANDED,
    SET_MAILBOX_FOLDERS,
    SET_UNREAD_COUNT
} from "~mutations";

export default {
    [ADD_FOLDER]: (state, { key, ...folder }) => {
        Vue.set(state, key, { ...folder, key });
    },
    [SET_MAILBOX_FOLDERS]: (state, { folders }) => {
        folders.forEach(folder => {
            Vue.set(state, folder.key, folder);
        });
    },
    [RENAME_FOLDER]: (state, { key, name, path }) => {
        state[key].name = name;
        state[key].path = path;
    },
    [REMOVE_FOLDER]: (state, { key }) => {
        Vue.delete(state, key);
    },
    [SET_UNREAD_COUNT]: (state, { key, unread }) => {
        state[key].unread = unread;
    },
    [SET_FOLDER_EXPANDED]: (state, { key, expanded }) => {
        state[key].expanded = expanded;
    },
    // Hooks
    [ADD_FLAG]: (state, { messages, flag }) => {
        if (flag === Flag.SEEN) {
            messages.forEach(({ folderRef: { key } }) => state[key] && state[key].unread--);
        }
    },
    [DELETE_FLAG]: (state, { messages, flag }) => {
        if (flag === Flag.SEEN) {
            messages.forEach(({ folderRef: { key } }) => state[key] && state[key].unread++);
        }
    },
    [SET_ACTIVE_FOLDER]: (state, folder) => {
        let parent = folder.parent && state[folder.parent];
        while (parent) {
            parent.expanded = true;
            parent = parent.parent && state[parent.parent];
        }
    }
};
