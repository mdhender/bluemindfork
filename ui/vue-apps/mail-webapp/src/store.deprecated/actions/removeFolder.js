export async function removeFolder({ commit, dispatch, rootState }, folderKey) {
    const folder = rootState.mail.folders[folderKey];
    const props = { oldFolder: folder };
    const root = { root: true };
    try {
        await dispatch("folders/remove", folderKey);
        commit("addApplicationAlert", { code: "MSG_FOLDER_REMOVE_SUCCESS", props }, root);
    } catch (e) {
        commit("addApplicationAlert", { code: "MSG_FOLDER_REMOVE_ERROR", props }, root);
    }
}
