import { STATUS } from "../constants";
import ItemUri from "@bluemind/item-uri";

export async function loadMessageList({ dispatch, commit, state, getters }, { folder, mailshare, filter, search }) {
    commit("setStatus", STATUS.LOADING);

    const locatedFolder = locateFolder(folder, mailshare, getters, state);

    commit("setMessageFilter", filter);
    commit("messages/clearItems");
    commit("messages/clearParts");
    commit("currentMessage/clear");
    commit("search/setStatus", "idle");
    commit("search/setPattern", search);
    commit("deleteAllSelectedMessages");

    await dispatch("selectFolder", locatedFolder.key);
    expandParents(commit, getters, locatedFolder);

    if (search) {
        await dispatch("search/search", { pattern: search, filter });
    } else {
        await dispatch("messages/list", { sorted: state.sorted, folderUid: locatedFolder.uid, filter });
        const sorted = state.messages.itemKeys;
        await dispatch("messages/multipleByKey", sorted.slice(0, 40));
    }

    commit("setStatus", STATUS.RESOLVED);
}

function locateFolder(local, mailshare, getters) {
    let folder;
    if (local || mailshare) {
        let keyOrPath = local || mailshare;
        folder = getters["folders/getFolderByKey"](keyOrPath);
        if (!folder) {
            let mailbox = local ? getters.my.mailboxUid : getMailshareUid(getters, mailshare);
            if (mailbox) {
                folder = getters["folders/getFolderByPath"](keyOrPath, mailbox);
            }
        }
    }
    return folder || getters.my.INBOX;
}

function getMailshareUid(getters, path) {
    const root = path
        .split("/")
        .filter(Boolean)
        .shift();
    const mailbox = getters.mailshares.filter(ms => ms.root === root).shift();
    return mailbox && mailbox.mailboxUid;
}

function expandParents(commit, getters, folder) {
    if (folder.value && folder.value.parentUid) {
        commit("expandFolder", folder.value.parentUid);
        const mailboxId = ItemUri.container(folder.key);
        const parentFolderKey = ItemUri.encode(folder.value.parentUid, mailboxId);
        const parentFolder = getters["folders/getFolderByKey"](parentFolderKey);
        expandParents(commit, getters, parentFolder);
    }
}
