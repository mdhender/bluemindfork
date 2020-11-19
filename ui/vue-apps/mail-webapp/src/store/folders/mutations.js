import { Flag } from "@bluemind/email";
import Vue from "vue";
import { RENAME_FOLDER } from "~actions";
import {
    ADD_FOLDER,
    ADD_FLAG,
    ADD_FOLDERS,
    DELETE_FLAG,
    REMOVE_FOLDER,
    REMOVE_MESSAGES,
    MOVE_MESSAGES,
    SET_UNREAD_COUNT,
    SET_FOLDER_EXPANDED
} from "~mutations";

export default {
    [ADD_FOLDER]: (state, { key, ...folder }) => {
        Vue.set(state, key, { ...folder, key });
    },
    [ADD_FOLDERS]: (state, folders) => {
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
    [REMOVE_MESSAGES]: (state, messages) => {
        messages.forEach(message => {
            // FIXME: FEATWEBML-1387 (crappy NOT_LOADED hack)
            if (message.date && !message.flags.includes(Flag.SEEN)) {
                const folder = state[message.folderRef.key];
                folder.unread--;
            }
        });
    },
    [MOVE_MESSAGES]: (state, { messages, folder }) => {
        messages.forEach(message => {
            // FIXME: FEATWEBML-1387
            if (message.date && !message.flags.includes(Flag.SEEN)) {
                state[message.folderRef.key].unread--;
                state[folder.key].unread++;
            }
        });
    }
};
