import UUIDGenerator from "@bluemind/uuid";

export async function createFolder({ commit, dispatch }, fullName) {
    const folder = { value: { fullName, path: fullName } };
    const uid = UUIDGenerator.generate();
    const props = { name: folder.value.fullName };
    const root = { root: true };
    commit("addApplicationAlert", { uid, code: "MSG_FOLDER_CREATE_LOADING", props }, root);
    try {
        await dispatch("$_createFolder", folder);
        commit("removeApplicationAlert", uid, root);
        addOkAlert(commit, root, folder, fullName);
    } catch (e) {
        commit("removeApplicationAlert", uid, { root: true });
        commit("addApplicationAlert", { code: "MSG_FOLDER_CREATE_ERROR", props }, root);
    }
}

function addOkAlert(commit, root, folder, newFolderFullName) {
    const props = {
        oldFolder: folder.value,
        folder: Object.assign({}, folder.value, { name: newFolderFullName }),
        folderNameLink: { name: "v:mail:home", params: { folder: newFolderFullName } }
    };
    commit("addApplicationAlert", { code: "MSG_FOLDER_CREATE_SUCCESS", props }, root);
}
