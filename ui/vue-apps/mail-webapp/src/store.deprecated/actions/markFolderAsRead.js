import { Flag } from "@bluemind/email";
import ItemUri from "@bluemind/item-uri";
import { inject } from "@bluemind/inject";
import { SET_UNREAD_COUNT } from "../../store/folders/mutations";

export async function markFolderAsRead(context, folderKey) {
    const folder = context.rootState.mail.folders[folderKey];
    const props = {
        folder: { name: folder.path },
        folderNameLink: { name: "v:mail:home", params: { folder: folderKey } }
    };
    try {
        await optimisticMarkFolderAsRead(context, folder);
        context.commit("addApplicationAlert", { code: "MSG_FOLDER_MARKASREAD_SUCCESS", props }, { root: true });
    } catch (e) {
        context.commit("addApplicationAlert", { code: "MSG_FOLDER_MARKASREAD_ERROR", props }, { root: true });
    }
}

async function optimisticMarkFolderAsRead(context, folder) {
    const messageKeys = unseenMessagesInFolder(context.state, folder.key);
    context.commit("messages/addFlag", { messageKeys, mailboxItemFlag: Flag.SEEN });
    const unreadCount = folder.unread;
    context.commit("mail/" + SET_UNREAD_COUNT, { key: folder.key, count: 0 }, { root: true });
    try {
        await inject("MailboxFoldersPersistence", folder.mailbox).markFolderAsRead(folder.id);
    } catch (e) {
        context.commit("messages/deleteFlag", { messageKeys, mailboxItemFlag: Flag.SEEN });
        context.commit("mail/" + SET_UNREAD_COUNT, { key: folder.key, count: unreadCount }, { root: true });
        throw e;
    }
}

function unseenMessagesInFolder(state, folderKey) {
    return Object.keys(state.messages.items).filter(
        key => ItemUri.container(key) === folderKey && !state.messages.items[key].value.flags.includes(Flag.SEEN)
    );
}
