import { DEFAULT_FOLDER_NAMES } from "./helpers/DefaultFolders";

export const FOLDER_BY_PATH = "FOLDER_BY_PATH";
const folderByPath = state => path => Object.values(state).find(folder => folder.path === path);

export const HAS_CHILDREN_GETTER = "HAS_CHILDREN_GETTER";
const hasChildreGetter = state => key => !!Object.values(state).find(folder => folder.parent === key);

export const MAILSHARE_FOLDERS = "MAILSHARE_FOLDERS";
const mailshareFolders = (state, { MAILSHARE_KEYS }) =>
    Object.values(state)
        .filter(folder => MAILSHARE_KEYS.includes(folder.mailboxRef.key))
        .map(folder => folder.key);

export const MY_MAILBOX_FOLDERS = "MY_MAILBOX_FOLDERS";
const myMailboxFolders = (state, { MY_MAILBOX_KEY }) =>
    Object.values(state)
        .filter(folder => MY_MAILBOX_KEY === folder.mailboxRef.key)
        .map(folder => folder.key);

const { INBOX, OUTBOX, DRAFTS, SENT, TRASH } = DEFAULT_FOLDER_NAMES;
const myGetterFor = folderName => (state, { MY_MAILBOX_KEY }) =>
    Object.values(state).find(folder => folder.mailboxRef.key === MY_MAILBOX_KEY && folder.imapName === folderName);

export const MY_INBOX = "MY_INBOX";
const myInbox = myGetterFor(INBOX);

export const MY_OUTBOX = "MY_OUTBOX";
const myOutbox = myGetterFor(OUTBOX);

export const MY_DRAFTS = "MY_DRAFTS";
const myDrafts = myGetterFor(DRAFTS);

export const MY_SENT = "MY_SENT";
const mySent = myGetterFor(SENT);

export const MY_TRASH = "MY_TRASH";
const myTrash = myGetterFor(TRASH);

export default {
    [FOLDER_BY_PATH]: folderByPath,
    [HAS_CHILDREN_GETTER]: hasChildreGetter,
    [MAILSHARE_FOLDERS]: mailshareFolders,
    [MY_MAILBOX_FOLDERS]: myMailboxFolders,
    [MY_INBOX]: myInbox,
    [MY_OUTBOX]: myOutbox,
    [MY_DRAFTS]: myDrafts,
    [MY_SENT]: mySent,
    [MY_TRASH]: myTrash
};
