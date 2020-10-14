import { ItemFlag } from "@bluemind/core.container.api";
import api from "../api/apiFolders";
import {
    SET_MAILBOX_FOLDERS,
    ADD_FOLDER,
    //TODO: change mutation names
    REMOVE_FOLDER as MUTATION_REMOVE_FOLDER,
    RENAME_FOLDER as MUTATION_RENAME_FOLDER,
    REMOVE_FOLDER,
    RENAME_FOLDER,
    SET_UNREAD_COUNT
} from "~/mutations";
import { FOLDERS_BY_UPPERCASE_PATH } from "~/getters";
import { FolderAdaptor } from "./helpers/FolderAdaptor";
import { create, rename } from "~/model/folder";
import { withAlert } from "../helpers/withAlert";
import {
    CREATE_FOLDER,
    EMPTY_FOLDER,
    FETCH_FOLDERS,
    CREATE_FOLDER_HIERARCHY,
    MARK_FOLDER_AS_READ,
    UNREAD_FOLDER_COUNT
} from "~/actions";

const fetchFolders = async function ({ commit }, mailbox) {
    const items = await api.getAllFolders(mailbox);
    const folders = items
        .filter(item => !item.flags.includes(ItemFlag.Deleted))
        .sort((a, b) => a.value.fullName.toLowerCase().localeCompare(b.value.fullName.toLowerCase()))
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
    let created = getters[FOLDERS_BY_UPPERCASE_PATH][folder.path.toUpperCase()];
    if (!created) {
        const item = FolderAdaptor.toMailboxFolder(folder, mailbox);
        const { uid, id: internalId } = await api.createNewFolder(mailbox, item);
        created = { ...folder, key: uid, remoteRef: { uid, internalId } };
        commit(ADD_FOLDER, created);
    }
    return created;
};

const removeFolder = async function ({ commit }, { folder, mailbox }) {
    commit(MUTATION_REMOVE_FOLDER, folder);
    try {
        await api.deleteFolder(mailbox, folder);
    } catch (e) {
        commit(ADD_FOLDER, folder);
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

const emptyFolder = async function ({ commit }, { folder }) {
    commit(SET_UNREAD_COUNT, { ...folder, unread: 0 });
};

const unreadFolderCount = async function ({ commit }, folder) {
    const unread = await api.unreadCount(folder);
    commit(SET_UNREAD_COUNT, { key: folder.key, unread: unread.total });
};

export default {
    [FETCH_FOLDERS]: fetchFolders,
    [CREATE_FOLDER]: withAlert(createFolderHierarchy, CREATE_FOLDER, "CreateFolder"),
    [CREATE_FOLDER_HIERARCHY]: createFolderHierarchy,
    [EMPTY_FOLDER]: emptyFolder,
    // FIXME: when deleting a folder having children, it is not deleted in UI & we got errors in the console..
    [REMOVE_FOLDER]: withAlert(removeFolder, REMOVE_FOLDER, "RemoveFolder"),
    [RENAME_FOLDER]: withAlert(renameFolder, RENAME_FOLDER, "RenameFolder"),
    [MARK_FOLDER_AS_READ]: withAlert(markFolderAsRead, MARK_FOLDER_AS_READ, "MarkFolderAsRead"),
    [UNREAD_FOLDER_COUNT]: unreadFolderCount
};
