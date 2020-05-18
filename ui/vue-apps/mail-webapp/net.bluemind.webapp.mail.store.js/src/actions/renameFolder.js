import UUIDGenerator from "@bluemind/uuid";

export async function renameFolder({ commit, dispatch, getters }, { folderKey, newFolderName }) {
    const folder = getters["folders/getFolderByKey"](folderKey);
    const props = { oldName: folder.value.name, newName: newFolderName };
    const root = { root: true };
    const uid = UUIDGenerator.generate();
    commit("addApplicationAlert", { uid, code: "MSG_FOLDER_RENAME_LOADING", props }, root);

    try {
        await dispatch("folders/rename", { folderKey, newFolderName });
        commit("removeApplicationAlert", uid, root);
        addOkAlert(commit, root, folder, newFolderName);
    } catch (e) {
        commit("removeApplicationAlert", uid, root);
        commit("addApplicationAlert", { code: "MSG_FOLDER_RENAME_ERROR", props }, root);
    }
}

function addOkAlert(commit, root, folder, newFolderName) {
    const newFolderFullName = computeNewFolderFullName(folder, newFolderName);
    const props = {
        oldFolder: folder.value,
        folder: Object.assign({}, folder.value, { name: newFolderName }),
        folderNameLink: { name: "v:mail:home", params: { folder: newFolderFullName } }
    };
    commit("addApplicationAlert", { code: "MSG_FOLDER_RENAME_SUCCESS", props }, root);
}

function computeNewFolderFullName(folder, newFolderName) {
    const oldPath = folder.value.fullName;
    const lastSlashIndex = oldPath.lastIndexOf("/");
    const pathPrefix = lastSlashIndex >= 0 ? oldPath.substring(0, lastSlashIndex + 1) : "";
    return pathPrefix + newFolderName;
}
