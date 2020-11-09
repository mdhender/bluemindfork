import { FolderAdaptor } from "../../store/folders/helpers/FolderAdaptor";
import { RENAME_FOLDER } from "~actions";

export async function renameFolder({ commit, dispatch, rootState }, { folderKey, newFolderName }) {
    const folder = rootState.mail.folders[folderKey];
    const mailbox = rootState.mail.mailboxes[folder.mailboxRef.key];
    const props = { oldName: folder.name, newName: newFolderName };

    const newFolder = FolderAdaptor.rename(folder, newFolderName);
    try {
        await dispatch("mail/" + RENAME_FOLDER, { folder: newFolder, mailbox }, { root: true });
        addOkAlert(commit, folder, newFolderName);
    } catch (e) {
        commit("alert/addApplicationAlert", { code: "MSG_FOLDER_RENAME_ERROR", props }, { root: true });
    }
}

function addOkAlert(commit, folder, newFolderName) {
    const props = {
        oldFolder: folder,
        folder: Object.assign({}, folder, { name: newFolderName }),
        folderNameLink: { name: "v:mail:home", params: { folder: folder.key } }
    };
    commit("alert/addApplicationAlert", { code: "MSG_FOLDER_RENAME_SUCCESS", props }, { root: true });
}
