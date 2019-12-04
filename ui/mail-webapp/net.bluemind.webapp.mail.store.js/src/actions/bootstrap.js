import { Verb } from "@bluemind/core.container.api";
export function bootstrap({ dispatch, state, getters, commit }, login) {
    commit("setUserLogin", login);

    return dispatch("folders/all", getters.my.mailboxUid)
        .then(() => {
            if (!state.currentFolderKey) {
                return dispatch("selectFolder", getters.my.INBOX.key);
            }
        })
        .then(() => getters.my.folders.forEach(folder => dispatch("loadUnreadCount", folder.uid)))
        .then(() => {
            dispatch("mailboxes/all", { verb: [Verb.Read, Verb.Write], type: "mailboxacl" }).then(() => {
                getters.mailshares.forEach(mailshare => dispatch("folders/all", mailshare.mailboxUid));
            });
        });
}
