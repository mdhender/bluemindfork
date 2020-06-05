import { Verb } from "@bluemind/core.container.api";

export function bootstrap({ dispatch, state, getters, commit }, login) {
    commit("setUserLogin", login);

    return dispatch("folders/all", getters.my.mailboxUid)
        .then(() => {
            if (!state.currentFolderKey) {
                return dispatch("loadMessageList", { folder: getters.my.INBOX.key, filter: state.messageFilter });
            }
        })
        .then(() => dispatch("mailboxes/all", { verb: [Verb.Read, Verb.Write, Verb.All], type: "mailboxacl" }))
        .then(() => Promise.all(getters.mailshares.map(mailshare => dispatch("folders/all", mailshare.mailboxUid))))
        .then(() => dispatch("loadUserSettings"))
        .then(() => dispatch("loadMailboxConfig"))
        .catch(() => {
            console.log("Failure occurred but bootstrap must not fail");
        });
}
