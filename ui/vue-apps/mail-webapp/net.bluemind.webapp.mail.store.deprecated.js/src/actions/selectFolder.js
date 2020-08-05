export function selectFolder({ commit, dispatch, rootState }, folder) {
    if (rootState.mail.activeFolder !== folder.key) {
        commit("mail/SET_CURRENT_FOLDER", folder.key, { root: true });
    }
    return dispatch("loadUnreadCount", folder.key);
}
