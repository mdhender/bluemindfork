import { RENAME_FOLDER } from "../../store/folders/actions";

export async function renameFolder({ commit, dispatch, rootState }, { folderKey, newFolderName }) {
    const folder = rootState.mail.folders[folderKey];
    const mailbox = rootState.mail.mailboxes[folder.mailbox];
    const props = { oldName: folder.name, newName: newFolderName };

    try {
        await dispatch("mail/" + RENAME_FOLDER, { key: folderKey, name: newFolderName, mailbox }, { root: true });
        addOkAlert(commit, folder, newFolderName);
    } catch (e) {
        commit("addApplicationAlert", { code: "MSG_FOLDER_RENAME_ERROR", props }, { root: true });
    }
}

function addOkAlert(commit, folder, newFolderName) {
    const props = {
        oldFolder: folder,
        folder: Object.assign({}, folder, { name: newFolderName }),
        folderNameLink: { name: "v:mail:home", params: { folder: folder.key } }
    };
    commit("addApplicationAlert", { code: "MSG_FOLDER_RENAME_SUCCESS", props }, { root: true });
}
