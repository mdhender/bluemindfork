import { REMOVE_FOLDER } from "../../store/folders/actions";

export async function removeFolder({ commit, dispatch, rootState }, folderKey) {
    const folder = rootState.mail.folders[folderKey];
    const mailbox = rootState.mail.mailboxes[folder.mailbox];
    const props = { oldFolder: folder };
    try {
        await dispatch("mail/" + REMOVE_FOLDER, { key: folderKey, mailbox }, { root: true });
        commit("addApplicationAlert", { code: "MSG_FOLDER_REMOVE_SUCCESS", props }, { root: true });
    } catch (e) {
        commit("addApplicationAlert", { code: "MSG_FOLDER_REMOVE_ERROR", props }, { root: true });
    }
}
