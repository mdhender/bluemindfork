import Vue from "vue";
import { FolderAdaptor } from "./helpers/FolderAdaptor";
import { inject } from "@bluemind/inject";
import { ItemFlag } from "@bluemind/core.container.api";

export const ADD_FOLDER = "ADD_FOLDER";
export const ADD_FOLDERS = "ADD_FOLDERS";
export const RENAME_FOLDER = "RENAME_FOLDER";
export const REMOVE_FOLDER = "REMOVE_FOLDER";
export const SET_UNREAD_COUNT = "SET_UNREAD_COUNT";
export const TOGGLE_FOLDER = "TOGGLE_FOLDER";

export const FETCH_FOLDERS = "FETCH_FOLDERS";
export const CREATE_FOLDER = "CREATE_FOLDER";

export const state = {
    folders: {}
};

export const getters = {
    ["HAS_CHILDREN_GETTER"]: state => key => !!Object.values(state.folders).find(folder => folder.parent === key),
    ["MAILSHARE_FOLDERS"]: (state, getters) => {
        return Object.values(state.folders)
            .filter(folder => getters["MAILSHARE_KEYS"].includes(folder.mailbox))
            .map(folder => folder.key);
    },
    ["MY_MAILBOX_FOLDERS"]: (state, getters) =>
        Object.values(state.folders)
            .filter(folder => getters["MY_MAILBOX_KEY"] === folder.mailbox)
            .map(folder => folder.key)
};

export const mutations = {
    [CREATE_FOLDER]: (state, { key, name, parent, mailbox }) => {
        const folder = FolderAdaptor.create(key, name, parent && state.folders[parent], mailbox);
        Vue.set(state.folders, folder.key, folder);
    },
    [ADD_FOLDER]: (state, folder) => {
        Vue.set(state.folders, folder.key, folder);
    },
    [ADD_FOLDERS]: (state, folders) => folders.forEach(folder => Vue.set(state.folders, folder.key, folder)),
    [RENAME_FOLDER]: (state, { key, name }) => {
        Vue.set(state.folders, key, FolderAdaptor.rename(state.folders[key], name));
    },
    [REMOVE_FOLDER]: (state, key) => {
        Vue.delete(state.folders, key);
    },
    [SET_UNREAD_COUNT]: (state, { key, count }) => {
        if (count >= 0 && state.folders[key].unread !== count) {
            state.folders[key].unread = count;
        }
    },
    [TOGGLE_FOLDER]: (state, key) => {
        state.folders[key].expanded = !state.folders[key].expanded;
    }
};

export const actions = {
    async [FETCH_FOLDERS]({ commit }, mailbox) {
        const items = await inject("MailboxFoldersPersistence", mailbox.uid).all();
        const folders = items
            .filter(item => !item.flags.includes(ItemFlag.Deleted))
            .sort((a, b) => a.value.fullName.toLowerCase().localeCompare(b.value.fullName.toLowerCase()))
            .map(item => FolderAdaptor.fromMailboxFolder(item, mailbox));
        commit(ADD_FOLDERS, folders);
    },

    async [CREATE_FOLDER]({ commit, state }, { key, name, parent, mailbox }) {
        commit(CREATE_FOLDER, { key, name, parent, mailbox });
        const folder = state.folders[key];
        const item = FolderAdaptor.toMailboxFolder(folder, state.folders[folder.parent], mailbox);
        try {
            const { uid, id } = await inject("MailboxFoldersPersistence", mailbox.uid).createBasic(item.value);
            commit(ADD_FOLDER, { ...folder, uid, id });
        } catch (e) {
            commit(REMOVE_FOLDER, key);
            throw e;
        }
    },
    //FIXME : when deleting a folder having children, it is not deleted in UI & we got errors in the console..
    async [REMOVE_FOLDER]({ state, commit }, { key, mailbox }) {
        const folder = state.folders[key];
        commit(REMOVE_FOLDER, key);
        try {
            const service = inject("MailboxFoldersPersistence", mailbox.uid);
            await service.deepDelete(folder.id);
        } catch (e) {
            commit(ADD_FOLDER, folder);
            throw e;
        }
    },
    async [RENAME_FOLDER]({ commit, state }, { name, key, mailbox }) {
        const oldName = state.folders[key].name;
        commit(RENAME_FOLDER, { name, key, mailbox });
        const folder = state.folders[key];
        const item = FolderAdaptor.toMailboxFolder(folder, state.folders[folder.parent], mailbox);
        try {
            await inject("MailboxFoldersPersistence", mailbox.uid).updateById(item.internalId, item.value);
        } catch (e) {
            commit(RENAME_FOLDER, { name: oldName, key, mailbox });
            throw e;
        }
    }
};
