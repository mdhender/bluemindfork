import UUIDGenerator from "@bluemind/uuid";

export async function createFolder({ commit, dispatch }, { folder, mailboxUid }) {
    const uid = UUIDGenerator.generate();
    const props = { name: folder.path };
    const root = { root: true };
    commit("addApplicationAlert", { uid, code: "MSG_FOLDER_CREATE_LOADING", props }, root);
    try {
        const folderKey = await dispatch("$_createFolder", { folder, mailboxUid });
        commit("removeApplicationAlert", uid, root);
        addOkAlert(commit, root, folder, folderKey);
    } catch (e) {
        commit("removeApplicationAlert", uid, root);
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
