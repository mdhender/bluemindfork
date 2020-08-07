export async function renameFolder({ commit, dispatch, rootState }, { folderKey, newFolderName }) {
    const folder = rootState.mail.folders[folderKey];
    const props = { oldName: folder.name, newName: newFolderName };
    const root = { root: true };

    try {
        await dispatch("folders/rename", { folder, newFolderName });
        addOkAlert(commit, root, folder, newFolderName);
    } catch (e) {
        commit("addApplicationAlert", { code: "MSG_FOLDER_RENAME_ERROR", props }, root);
    }
}

function addOkAlert(commit, root, folder, newFolderName) {
    const props = {
        oldFolder: folder,
        folder: Object.assign({}, folder, { name: newFolderName }),
        folderNameLink: { name: "v:mail:home", params: { folder: folder.key } }
    };
    commit("addApplicationAlert", { code: "MSG_FOLDER_RENAME_SUCCESS", props }, root);
}
