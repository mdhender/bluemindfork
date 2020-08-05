import UUIDGenerator from "@bluemind/uuid";
import { Flag } from "@bluemind/email";
import ItemUri from "@bluemind/item-uri";
import { SET_UNREAD_COUNT } from "../../store/";

export async function markFolderAsRead(context, folderKey) {
    const folder = context.rootState.mail.folders[folderKey];
    const uid = UUIDGenerator.generate();
    const props = {
        folder: { name: folder.path },
        folderNameLink: { name: "v:mail:home", params: { folder: folderKey } }
    };
    const root = { root: true };
    context.commit("addApplicationAlert", { uid, code: "MSG_FOLDER_MARKASREAD_LOADING", props }, root);
    try {
        await optimisticMarkFolderAsRead(context, folder);
        context.commit("removeApplicationAlert", uid, root);
        context.commit("addApplicationAlert", { code: "MSG_FOLDER_MARKASREAD_SUCCESS", props }, root);
    } catch (e) {
        context.commit("removeApplicationAlert", uid, root);
        context.commit("addApplicationAlert", { code: "MSG_FOLDER_MARKASREAD_ERROR", props }, root);
    }
}

async function optimisticMarkFolderAsRead(context, folder) {
    const messageKeys = unseenMessagesInFolder(context.state, folder.key);
    context.commit("messages/addFlag", { messageKeys, mailboxItemFlag: Flag.SEEN });
    const unreadCount = folder.unread;
    context.commit(SET_UNREAD_COUNT, { key: folder.key, count: 0 }, { root: true });
    try {
        await context.dispatch("folders/markAsRead", folder.key);
    } catch (e) {
        context.commit("messages/deleteFlag", { messageKeys, mailboxItemFlag: Flag.SEEN });
        context.commit(SET_UNREAD_COUNT, { key: folder.key, count: unreadCount }, { root: true });
        throw e;
    }
}

function unseenMessagesInFolder(state, folderKey) {
    return Object.keys(state.messages.items).filter(
        key => ItemUri.container(key) === folderKey && !state.messages.items[key].value.flags.includes(Flag.SEEN)
    );
}
