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
        commit("addApplicationAlert", { code: "MSG_FOLDER_CREATE_SUCCESS", props }, root);
    } catch (e) {
        commit("removeApplicationAlert", uid, { root: true });
        commit("addApplicationAlert", { code: "MSG_FOLDER_CREATE_ERROR", props }, root);
    }
}
