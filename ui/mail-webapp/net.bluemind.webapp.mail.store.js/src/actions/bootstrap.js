export function bootstrap({ dispatch, state, getters, commit }, login) {
    commit("setUserLogin", login);
    getters.my.mailboxUid;
    return dispatch("folders/all", getters.my.mailboxUid)
        .then(() => {
            if (!state.currentFolderKey) {
                return dispatch("selectFolder", getters.my.INBOX.key);
            }
        })
        .then(() => {
            getters.my.folders.forEach(folder => dispatch("loadUnreadCount", folder.uid));
        });
}
