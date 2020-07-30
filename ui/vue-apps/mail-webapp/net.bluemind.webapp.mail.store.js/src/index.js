export { FolderAdaptor } from "./helpers/FolderAdaptor";
export { MailboxAdaptor } from "./helpers/MailboxAdaptor";
import * as folders from "./folders";
import * as folderList from "./folderList";
import * as mailboxes from "./mailboxes";

export default {
    namespaced: true,
    state: { ...folders.state, ...folderList.state, ...mailboxes.state },
    actions: { ...folders.actions, ...mailboxes.actions },
    mutations: { ...folders.mutations, ...folderList.mutations, ...mailboxes.mutations }
};

export const ADD_FOLDER = "mail/" + folders.ADD_FOLDER;
export const ADD_FOLDERS = "mail/" + folders.ADD_FOLDERS;
export const FETCH_FOLDERS = "mail/" + folders.FETCH_FOLDERS;
export const CREATE_FOLDER = "mail/" + folders.CREATE_FOLDER;
export const RENAME_FOLDER = "mail/" + folders.RENAME_FOLDER;
export const REMOVE_FOLDER = "mail/" + folders.REMOVE_FOLDER;
export const SET_UNREAD_COUNT = "mail/" + folders.SET_UNREAD_COUNT;
export const TOGGLE_FOLDER = "mail/" + folders.TOGGLE_FOLDER;

export const TOGGLE_EDIT_FOLDER = "mail/" + folderList.TOGGLE_EDIT_FOLDER;

export const ADD_MAILBOXES = "mail/" + mailboxes.ADD_FOLDER;
export const FETCH_MAILBOXES = "mail/" + mailboxes.FETCH_MAILBOXES;
