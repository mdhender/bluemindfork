import { ItemFlag } from "@bluemind/core.container.api";
import api from "../api/apiFolders";
import {
    ADD_FOLDER,
    RENAME_FOLDER,
    SET_MAILBOX_FOLDERS,
    SET_UNREAD_COUNT,
    //TODO: change mutation names
    REMOVE_FOLDER as MUTATION_REMOVE_FOLDER,
    RENAME_FOLDER as MUTATION_RENAME_FOLDER,
    MOVE_FOLDER as MUTATION_MOVE_FOLDER
} from "~/mutations";
import { FOLDER_BY_PATH, FOLDER_GET_DESCENDANTS } from "~/getters";
import { FolderAdaptor } from "./helpers/FolderAdaptor";
import { create, rename, move } from "~/model/folder";
import { withAlert } from "../helpers/withAlert";
import {
    CREATE_FOLDER_HIERARCHY,
    CREATE_FOLDER,
    EMPTY_FOLDER,
    EMPTY_TRASH,
    FETCH_FOLDERS,
    MARK_FOLDER_AS_READ,
    MOVE_FOLDER,
    REMOVE_FOLDER,
    UNREAD_FOLDER_COUNT
} from "~/actions";

const fetchFolders = async function ({ commit }, mailbox) {
    const items = await api.getAllFolders(mailbox);
    const folders = items
        .filter(item => !item.flags.includes(ItemFlag.Deleted))
        .map(item => FolderAdaptor.fromMailboxFolder(item, mailbox));
    commit(SET_MAILBOX_FOLDERS, { folders, mailbox });
};

const createFolderHierarchy = async function ({ commit, getters, dispatch }, { name, parent, mailbox }) {
    const hierarchy = name.split("/").filter(Boolean);
    name = hierarchy.pop();
    if (hierarchy.length > 0) {
        parent = await dispatch(CREATE_FOLDER_HIERARCHY, { name: hierarchy.join("/"), parent, mailbox });
    }
    const folder = create(undefined, name, parent, mailbox);
    let created = getters[FOLDER_BY_PATH](folder.path, mailbox);
    if (!created) {
        const item = FolderAdaptor.toMailboxFolder(folder, mailbox);
        const { uid, id: internalId } = await api.createNewFolder(mailbox, item);
        created = { ...folder, key: uid, remoteRef: { uid, internalId } };
        commit(ADD_FOLDER, created);
    }
    return created;
};

const removeFolder = async function ({ commit, getters }, { folder, mailbox }) {
    const descendants = getters[FOLDER_GET_DESCENDANTS](folder);
    const toBeRemoved = descendants.concat(folder);

    toBeRemoved.forEach(folderToRemove => {
        commit(MUTATION_REMOVE_FOLDER, folderToRemove);
    });
    try {
        await api.deleteFolder(mailbox, folder);
    } catch (e) {
        toBeRemoved.forEach(folderToRemove => {
            commit(ADD_FOLDER, folderToRemove);
        });
        throw e;
    }
};

const moveFolder = async function ({ commit, state }, { folder, parent, mailbox }) {
    const { parent: oldParent, path: oldPath } = folder;
    parent = state[parent.key];
    const moved = move(folder, parent, mailbox);
    commit(MUTATION_MOVE_FOLDER, moved);

    const item = FolderAdaptor.toMailboxFolder(moved, mailbox);
    try {
        await api.updateFolder(mailbox, item);
    } catch (e) {
        commit(MUTATION_MOVE_FOLDER, { key: folder.key, parent: oldParent, path: oldPath });
        throw e;
    }
};

const renameFolder = async function ({ commit }, { folder, name, mailbox }) {
    const { name: oldName, path: oldPath } = folder;
    const renamed = rename(folder, name);
    commit(MUTATION_RENAME_FOLDER, renamed);

    const item = FolderAdaptor.toMailboxFolder(renamed, mailbox);
    try {
        await api.updateFolder(mailbox, item);
    } catch (e) {
        commit(MUTATION_RENAME_FOLDER, { name: oldName, key: folder.key, path: oldPath });
        throw e;
    }
};

const markFolderAsRead = async function ({ commit }, { folder, mailbox }) {
    const unread = folder.unread;
    commit(SET_UNREAD_COUNT, { ...folder, unread: 0 });
    try {
        await api.markAsRead(mailbox, folder);
    } catch (e) {
        commit(SET_UNREAD_COUNT, { key: folder.key, unread });
        throw e;
    }
};

const emptyFolder = async function ({ commit, getters }, { folder, deep }) {
    commit(SET_UNREAD_COUNT, { ...folder, unread: 0 });
    if (deep) {
        const descendants = getters[FOLDER_GET_DESCENDANTS](folder);
        descendants.forEach(folderToRemove => {
            commit(MUTATION_REMOVE_FOLDER, folderToRemove);
        });
    }
};

const unreadFolderCount = async function ({ commit }, folder) {
    if (folder.unread === undefined) {
        commit(SET_UNREAD_COUNT, { key: folder.key, unread: 0 });
    }
    const unread = await api.unreadCount(folder);
    commit(SET_UNREAD_COUNT, { key: folder.key, unread: unread.total });
};

export default {
    [FETCH_FOLDERS]: fetchFolders,
    [CREATE_FOLDER]: withAlert(createFolderHierarchy, CREATE_FOLDER, "CreateFolder"),
    [CREATE_FOLDER_HIERARCHY]: createFolderHierarchy,
    [EMPTY_FOLDER]: emptyFolder,
    [EMPTY_TRASH]: emptyFolder,
    [REMOVE_FOLDER]: withAlert(removeFolder, REMOVE_FOLDER, "RemoveFolder"),
    [MOVE_FOLDER]: withAlert(moveFolder, MOVE_FOLDER, "MoveFolder"),
    [RENAME_FOLDER]: withAlert(renameFolder, RENAME_FOLDER, "RenameFolder"),
    [MARK_FOLDER_AS_READ]: withAlert(markFolderAsRead, MARK_FOLDER_AS_READ, "MarkFolderAsRead"),
    [UNREAD_FOLDER_COUNT]: unreadFolderCount
};
