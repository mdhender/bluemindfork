import UUIDGenerator from "@bluemind/uuid";

export async function renameFolder({ commit, dispatch, getters }, { folderKey, newFolderName }) {
    const folder = getters["folders/getFolderByKey"](folderKey);
    const props = { oldName: folder.value.name, newName: newFolderName };

    const uid = UUIDGenerator.generate();
    commit("addApplicationAlert", { uid, code: "MSG_FOLDER_RENAME_LOADING", props }, { root: true });

    try {
        await dispatch("folders/rename", { folderKey, newFolderName });
        commit("removeApplicationAlert", uid, { root: true });
        commit("addApplicationAlert", { code: "MSG_FOLDER_RENAME_SUCCESS", props }, { root: true });
    } catch (e) {
        commit("removeApplicationAlert", uid, { root: true });
        commit("addApplicationAlert", { code: "MSG_FOLDER_RENAME_ERROR", props }, { root: true });
    }
}
