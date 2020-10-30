import ContainerObserver from "@bluemind/containerobserver";
import SearchHelper from "../SearchHelper";
import router from "@bluemind/router";
import { FOLDER_BY_PATH } from "../../store/folders/getters";
import { TOGGLE_FOLDER } from "../../store/folders/mutations";
import mutationTypes from "../../store/mutationTypes";
import actionTypes from "../../store/actionTypes";
import { FolderAdaptor } from "../../store/folders/helpers/FolderAdaptor";

export async function loadMessageList(
    { dispatch, commit, rootState, rootGetters },
    { folder, mailshare, filter, search }
) {
    const ROOT = { root: true };
    const locatedFolder = locateFolder(folder, mailshare, rootState, rootGetters);
    commit("mail/SET_ACTIVE_FOLDER", locatedFolder.key, ROOT);
    dispatch("loadUnreadCount", locatedFolder.key);
    expandParents(commit, locatedFolder, rootState);

    const searchInfo = SearchHelper.parseQuery(search);
    if (search) {
        if (
            SearchHelper.isSameSearch(
                rootState.mail.messageList.search.pattern,
                rootState.mail.messageList.search.folder && rootState.mail.messageList.search.folder.key,
                searchInfo.pattern,
                searchInfo.folder,
                filter,
                rootState.mail.messageList.filter
            )
        ) {
            return;
        }
    }
    const searchFolder = searchInfo.folder ? FolderAdaptor.toRef(rootState.mail.folders[searchInfo.folder]) : undefined;
    commit("mail/" + mutationTypes.SET_MESSAGE_LIST_FILTER, filter, ROOT);
    commit("mail/" + mutationTypes.CLEAR_MESSAGE_LIST, null, ROOT);
    commit("mail/" + mutationTypes.SET_SEARCH_PATTERN, searchInfo.pattern, ROOT);
    commit("mail/" + mutationTypes.SET_SEARCH_FOLDER, searchFolder, ROOT);

    commit("currentMessage/clear");

    const prefix = "mbox_records_";
    const previousFolderKey = rootState.mail.activeFolder;
    if (previousFolderKey) {
        ContainerObserver.forget("mailbox_records", prefix + previousFolderKey);
    }
    if (!rootGetters["mail/MESSAGE_LIST_IS_SEARCH_MODE"]) {
        ContainerObserver.observe("mailbox_records", prefix + rootState.mail.activeFolder);
    }
    const f = rootState.mail.folders[locatedFolder.key];
    const conversationsEnabled = rootState.session.userSettings.mail_thread === "true";
    await dispatch("mail/" + actionTypes.FETCH_MESSAGE_LIST_KEYS, { folder: f, conversationsEnabled }, ROOT);
    const sorted = rootState.mail.messageList.messageKeys;
    await dispatch("mail/" + actionTypes.FETCH_MESSAGE_METADATA, { messageKeys: sorted.slice(0, 40) }, ROOT);
}

function locateFolder(local, mailshare, rootState, rootGetters) {
    let folder;
    if (local || mailshare) {
        let keyOrPath = local || mailshare;
        if (rootState.mail.folders[keyOrPath]) {
            folder = rootState.mail.folders[keyOrPath];
        } else {
            folder = rootGetters["mail/" + FOLDER_BY_PATH](keyOrPath);
        }
        if (!folder) {
            router.push({ name: "mail:root" });
        }
    }
    return folder || rootGetters["mail/MY_INBOX"];
}

function expandParents(commit, folder, rootState) {
    if (folder.parent) {
        const parentFolder = rootState.mail.folders[folder.parent];
        if (!parentFolder.expanded) {
            commit("mail/" + TOGGLE_FOLDER, folder.parent, { root: true });
        }
        expandParents(commit, parentFolder, rootState);
    }
}
