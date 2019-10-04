export function bootstrap({ dispatch, state, getters }) {
    return dispatch("folders/all")
        .then(() => {
            if (!state.currentFolderUid) {
                return dispatch("selectFolder", getters["folders/defaultFolders"].INBOX.uid);
            }
        })
        .then(() => {
            state.folders.items.forEach(folder => {
                dispatch("loadUnreadCount", folder.uid);
            });
        });
}
