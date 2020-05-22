import UUIDGenerator from "@bluemind/uuid";
import { Flag } from "@bluemind/email";
import ItemUri from "@bluemind/item-uri";

export async function markFolderAsRead(context, folderKey) {
    const folder = context.getters["folders/getFolderByKey"](folderKey);
    const uid = UUIDGenerator.generate();
    const props = {
        folder: { name: folder.value.fullName },
        folderNameLink: { name: "v:mail:home", params: { folder: folderKey } }
    };
    const root = { root: true };
    context.commit("addApplicationAlert", { uid, code: "MSG_FOLDER_MARKASREAD_LOADING", props }, root);
    try {
        await optimisticMarkFolderAsRead(context, folderKey);
        context.commit("removeApplicationAlert", uid, root);
        context.commit("addApplicationAlert", { code: "MSG_FOLDER_MARKASREAD_SUCCESS", props }, root);
    } catch (e) {
        context.commit("removeApplicationAlert", uid, root);
        context.commit("addApplicationAlert", { code: "MSG_FOLDER_MARKASREAD_ERROR", props }, root);
    }
}

async function optimisticMarkFolderAsRead(context, folderKey) {
    const folderUid = ItemUri.item(folderKey);
    const messageKeys = unseenMessagesInFolder(context.state, folderUid);
    context.commit("messages/addFlag", { messageKeys, mailboxItemFlag: Flag.SEEN });
    const unreadCount = context.getters.unreadCount(folderUid);
    context.commit("setUnreadCount", { folderUid, count: 0 });
    try {
        await context.dispatch("folders/markAsRead", folderKey);
    } catch (e) {
        context.commit("messages/deleteFlag", { messageKeys, mailboxItemFlag: Flag.SEEN });
        context.commit("setUnreadCount", { folderUid, count: unreadCount });
        throw e;
    }
}

function unseenMessagesInFolder(state, folderUid) {
    return Object.keys(state.messages.items).filter(
        key => ItemUri.container(key) === folderUid && !state.messages.items[key].value.flags.includes(Flag.SEEN)
    );
}
