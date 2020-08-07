export async function createFolder({ commit, dispatch }, { folder, mailboxUid }) {
    const props = { name: folder.path };
    const root = { root: true };
    try {
        const folderKey = await dispatch("$_createFolder", { folder, mailboxUid });
        addOkAlert(commit, root, folder, folderKey);
    } catch (e) {
        commit("addApplicationAlert", { code: "MSG_FOLDER_CREATE_ERROR", props }, root);
    }
}

function addOkAlert(commit, root, folder, folderKey) {
    const props = {
        oldFolder: folder,
        folder: Object.assign({}, folder, { name: folder.path }),
        folderNameLink: { name: "v:mail:home", params: { folder: folderKey } }
    };
    commit("addApplicationAlert", { code: "MSG_FOLDER_CREATE_SUCCESS", props }, root);
}
