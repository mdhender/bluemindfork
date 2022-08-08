import { Flag } from "@bluemind/email";
import Vue from "vue";
import {
    ADD_FLAG,
    ADD_FOLDER,
    DELETE_FLAG,
    REMOVE_FOLDER,
    SET_UNREAD_COUNT,
    UPDATE_FOLDER,
    UPDATE_PATHS
} from "~/mutations";

export default {
    [ADD_FOLDER]: (state, { key, ...folder }) => {
        Vue.set(state, key, { ...folder, key });
    },
    [UPDATE_FOLDER]: (state, { imapName, key, name, parent, path }) => {
        state[key].imapName = imapName;
        state[key].name = name;
        state[key].parent = parent;
        state[key].path = path;
    },
    [UPDATE_PATHS]: (state, { folders, initial, updated }) => {
        folders.forEach(folder => {
            if (state[folder.key].path.startsWith(initial.path)) {
                state[folder.key].path = state[folder.key].path.replace(initial.path, updated.path);
            }
        });
    },
    [REMOVE_FOLDER]: (state, { key }) => {
        Vue.delete(state, key);
    },
    [SET_UNREAD_COUNT]: (state, { key, unread }) => {
        state[key].unread = unread;
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
    }
};
