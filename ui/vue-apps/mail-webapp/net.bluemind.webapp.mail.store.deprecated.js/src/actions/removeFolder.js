import { ItemUri } from "@bluemind/item-uri";
import UUIDGenerator from "@bluemind/uuid";

export async function removeFolder({ commit, dispatch, getters, rootState }, folderUid) {
    const folderKey = ItemUri.encode(folderUid, rootState.mail.folders[folderUid].mailbox);
    const folder = getters["folders/getFolderByKey"](folderKey);
    const uid = UUIDGenerator.generate();
    const props = { oldFolder: folder.value };
    const root = { root: true };
    commit("addApplicationAlert", { uid, code: "MSG_FOLDER_REMOVE_LOADING", props }, root);
    try {
        await dispatch("folders/remove", folderKey);
        commit("removeApplicationAlert", uid, root);
        commit("addApplicationAlert", { code: "MSG_FOLDER_REMOVE_SUCCESS", props }, root);
    } catch (e) {
        commit("removeApplicationAlert", uid, root);
        commit("addApplicationAlert", { code: "MSG_FOLDER_REMOVE_ERROR", props }, root);
    }
}
