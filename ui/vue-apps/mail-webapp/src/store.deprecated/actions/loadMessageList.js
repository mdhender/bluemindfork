import { STATUS } from "../constants";
import { STATUS as SEARCH_STATUS } from "../modules/search";
import { TOGGLE_FOLDER } from "../../store/";
import ContainerObserver from "@bluemind/containerobserver";
import ItemUri from "@bluemind/item-uri";
import SearchHelper from "../SearchHelper";
import router from "@bluemind/router";

export async function loadMessageList(
    { dispatch, commit, state, rootState, rootGetters },
    { folder, mailshare, filter, search }
) {
    const locatedFolder = locateFolder(folder, mailshare, rootState, rootGetters);
    const locatedFolderIsMailshareRoot = mailshare && !locatedFolder.parent;
    await dispatch("selectFolder", locatedFolder);
    expandParents(commit, locatedFolder, rootState);

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
    const previousFolderKey = rootState.mail.activeFolder;
    if (previousFolderKey) {
        ContainerObserver.forget("mailbox_records", prefix + previousFolderKey);
    }

    if (search) {
        await dispatch("search/search", { pattern: searchInfo.pattern, filter, folderKey: searchInfo.folder });
    } else {
        ContainerObserver.observe("mailbox_records", prefix + rootState.mail.activeFolder);
        await dispatch("messages/list", { sorted: state.sorted, folderUid: locatedFolder.key, filter });
        const sorted = state.messages.itemKeys;
        await dispatch("messages/multipleByKey", sorted.slice(0, 40));
    }

    commit("setStatus", STATUS.RESOLVED);
}

function locateFolder(local, mailshare, rootState, rootGetters) {
    let folder;
    if (local || mailshare) {
        let keyOrPath = local || mailshare;
        if (rootState.mail.folders[keyOrPath]) {
            folder = rootState.mail.folders[keyOrPath];
        } else if (ItemUri.isItemUri(keyOrPath)) {
            console.error("SHOULD NOT HAPPEN ANYMORE, USE folderUid instead of folderKey in router");
            folder = rootState.mail.folders[ItemUri.item(keyOrPath)];
        } else {
            folder = rootGetters["mail/FOLDER_BY_PATH"](keyOrPath);
        }
        if (!folder) {
            router.push({ name: "mail:root" });
        }
    }
    return folder || rootGetters["mail/MY_DEFAULT_FOLDERS"].INBOX;
}

function expandParents(commit, folder, rootState) {
    if (folder.parent) {
        const parentFolder = rootState.mail.folders[folder.parent];
        if (!parentFolder.expanded) {
            commit(TOGGLE_FOLDER, folder.parent, { root: true });
        }
        expandParents(commit, parentFolder, rootState);
    }
}
