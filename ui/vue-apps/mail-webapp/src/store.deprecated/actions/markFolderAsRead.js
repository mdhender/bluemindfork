import { Flag } from "@bluemind/email";
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
    const keys = unseenMessagesInFolder(context.rootState, context.rootGetters, folder.key);
    context.commit("mail/" + mutationTypes.ADD_FLAG, { keys, flag: Flag.SEEN }, { root: true });
    const unreadCount = folder.unread;
    context.commit("mail/" + SET_UNREAD_COUNT, { key: folder.key, count: 0 }, { root: true });
    try {
        await inject("MailboxFoldersPersistence", folder.mailboxRef.uid).markFolderAsRead(folder.remoteRef.internalId);
    } catch (e) {
        context.commit("mail/" + mutationTypes.DELETE_FLAG, { keys, flag: Flag.SEEN }, { root: true });
        context.commit("mail/" + SET_UNREAD_COUNT, { key: folder.key, count: unreadCount }, { root: true });
        throw e;
    }
}

function unseenMessagesInFolder(rootState, rootGetters, folderKey) {
    return Object.values(rootState.mail.messages)
        .filter(
            message =>
                message.folderRef.key === folderKey &&
                rootGetters["mail/isLoaded"](message.key) &&
                !message.flags.includes(Flag.SEEN)
        )
        .map(message => message.key);
}
