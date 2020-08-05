import UUIDGenerator from "@bluemind/uuid";

export async function renameFolder({ commit, dispatch, rootState }, { folderKey, newFolderName }) {
    const folder = rootState.mail.folders[folderKey];
    const props = { oldName: folder.name, newName: newFolderName };
    const root = { root: true };
    const uid = UUIDGenerator.generate();
    commit("addApplicationAlert", { uid, code: "MSG_FOLDER_RENAME_LOADING", props }, root);

    try {
        await dispatch("folders/rename", { folder, newFolderName });
        commit("removeApplicationAlert", uid, root);
        addOkAlert(commit, root, folder, newFolderName);
    } catch (e) {
        commit("removeApplicationAlert", uid, root);
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
