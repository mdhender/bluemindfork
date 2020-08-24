import { ItemFlag } from "@bluemind/core.container.api";
import api from "../api/apiFolders";
import {
    ADD_FOLDERS,
    ADD_FOLDER,
    //TODO: change mutation names
    REMOVE_FOLDER as MUTATION_REMOVE_FOLDER,
    RENAME_FOLDER as MUTATION_RENAME_FOLDER
} from "./mutations";
import { FolderAdaptor } from "./helpers/FolderAdaptor";

export const FETCH_FOLDERS = "FETCH_FOLDERS";
const fetchFolders = async function ({ commit }, mailbox) {
    const items = await api.getAllFolders(mailbox);
    const folders = items
        .filter(item => !item.flags.includes(ItemFlag.Deleted))
        .sort((a, b) => a.value.fullName.toLowerCase().localeCompare(b.value.fullName.toLowerCase()))
        .map(item => FolderAdaptor.fromMailboxFolder(item, mailbox));
    commit(ADD_FOLDERS, folders);
};

export const CREATE_FOLDER = "CREATE_FOLDER";
const createFolder = async function ({ commit, state }, { key, name, parent, mailbox }) {
    const foldertoadd = FolderAdaptor.create(key, name, parent && state[parent], mailbox);
    commit(ADD_FOLDER, foldertoadd);
    const item = FolderAdaptor.toMailboxFolder(foldertoadd, mailbox);
    try {
        const { uid, id } = await api.createNewFolder(mailbox, item);
        commit(ADD_FOLDER, { ...foldertoadd, uid, id });
    } catch (e) {
        commit(MUTATION_REMOVE_FOLDER, foldertoadd.key);
        throw e;
    }
};

export const REMOVE_FOLDER = "REMOVE_FOLDER";
const removeFolder = async function ({ state, commit }, { key, mailbox }) {
    const folder = state[key];
    commit(MUTATION_REMOVE_FOLDER, folder.key);
    try {
        await api.deleteFolder(mailbox, folder);
    } catch (e) {
        commit(ADD_FOLDER, folder);
        throw e;
    }
};

export const RENAME_FOLDER = "RENAME_FOLDER";
const renameFolder = async function ({ commit, state }, { folder, mailbox }) {
    const { name: oldName, path: oldPath } = state[folder.key];
    commit(MUTATION_RENAME_FOLDER, { name: folder.name, key: folder.key, path: folder.path });
    const item = FolderAdaptor.toMailboxFolder(folder, mailbox);
    try {
        await api.updateFolder(mailbox, item);
    } catch (e) {
        commit(MUTATION_RENAME_FOLDER, { name: oldName, key: folder.key, path: oldPath });
        throw e;
    }
};
export default {
    [FETCH_FOLDERS]: fetchFolders,
    [CREATE_FOLDER]: createFolder,
    // FIXME: when deleting a folder having children, it is not deleted in UI & we got errors in the console..
    [REMOVE_FOLDER]: removeFolder,
    [RENAME_FOLDER]: renameFolder
};
