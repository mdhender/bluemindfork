import { Flag } from "@bluemind/email";
import ItemUri from "@bluemind/item-uri";
import { inject } from "@bluemind/inject";
import { SET_UNREAD_COUNT } from "../../store/folders/mutations";
import mutationTypes from "../../store/mutationTypes";

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
    const messageKeys = unseenMessagesInFolder(context.rootState, context.getters, folder.key);
    context.commit("mail/" + mutationTypes.ADD_FLAG, { messageKeys, flag: Flag.SEEN }, { root: true });
    const unreadCount = folder.unread;
    context.commit("mail/" + SET_UNREAD_COUNT, { key: folder.key, count: 0 }, { root: true });
    console.log(folder);
    try {
        await inject("MailboxFoldersPersistence", folder.mailboxRef.uid).markFolderAsRead(folder.remoteRef.internalId);
    } catch (e) {
        context.commit("mail/DELETE_FLAG", { messageKeys, flag: Flag.SEEN }, { root: true });
        context.commit("mail/" + SET_UNREAD_COUNT, { key: folder.key, count: unreadCount }, { root: true });
        throw e;
    }
}

function unseenMessagesInFolder(rootState, getters, folderKey) {
    return getters["messages/messages"].filter(
        message => ItemUri.container(message.key) === folderKey && message.flags.includes(Flag.SEEN)
    );
}
