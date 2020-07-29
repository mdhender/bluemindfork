import { STATUS } from "../constants";
import { STATUS as SEARCH_STATUS } from "../modules/search";
import { TOGGLE_FOLDER } from "@bluemind/webapp.mail.store";
import ContainerObserver from "@bluemind/containerobserver";
import ItemUri from "@bluemind/item-uri";
import SearchHelper from "../SearchHelper";

export async function loadMessageList({ dispatch, commit, state, getters }, { folder, mailshare, filter, search }) {
    const locatedFolder = locateFolder(folder, mailshare, getters);
    const locatedFolderIsMailshareRoot = mailshare && !locatedFolder.value.fullName.includes("/");
    await dispatch("selectFolder", locatedFolder.key);
    expandParents(commit, getters, locatedFolder);

    const searchInfo = SearchHelper.parseQuery(search);
    let searchStatus = SEARCH_STATUS.IDLE;
    if (search) {
        searchStatus = SEARCH_STATUS.LOADING;
        if (
            SearchHelper.isSameSearch(
                state.search.pattern,
                state.search.searchFolder,
                searchInfo.pattern,
                searchInfo.folder
            )
        ) {
            searchStatus = SEARCH_STATUS.RESOLVED;
        }
    }
    commit("search/setStatus", searchStatus);
    if (searchStatus === SEARCH_STATUS.RESOLVED) {
        return;
    }
    commit("setMessageFilter", filter);
    commit("messages/clearItems");
    commit("messages/clearParts");
    commit("currentMessage/clear");
    commit("setStatus", STATUS.LOADING);
    commit("search/setPattern", searchInfo.pattern);
    const searchFolder = searchInfo.folder
        ? searchInfo.folder
        : locatedFolderIsMailshareRoot
        ? locatedFolder.key
        : undefined;
    commit("search/setSearchFolder", searchFolder);
    commit("deleteAllSelectedMessages");

    const prefix = "mbox_records_";
    const previousFolderKey = state.currentFolderKey;
    if (previousFolderKey) {
        ContainerObserver.forget("mailbox_records", prefix + ItemUri.item(previousFolderKey));
    }

    if (search) {
        await dispatch("search/search", { pattern: searchInfo.pattern, filter, folderKey: searchInfo.folder });
    } else {
        ContainerObserver.observe("mailbox_records", prefix + ItemUri.item(state.currentFolderKey));
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
        if (ItemUri.isItemUri(keyOrPath)) {
            folder = getters["folders/getFolderByKey"](keyOrPath);
        }
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
        console.log("Expand parents : CALL TOGGLE_FOLDER");
        commit(TOGGLE_FOLDER, folder.value.parentUid, { root: true });
        const mailboxId = ItemUri.container(folder.key);
        const parentFolderKey = ItemUri.encode(folder.value.parentUid, mailboxId);
        const parentFolder = getters["folders/getFolderByKey"](parentFolderKey);
        expandParents(commit, getters, parentFolder);
    }
}
