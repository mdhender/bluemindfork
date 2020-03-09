import { Verb } from "@bluemind/core.container.api";
import injector from "@bluemind/inject";

export function bootstrap({ dispatch, state, getters, commit }, login) {
    commit("setUserLogin", login);

    return dispatch("folders/all", getters.my.mailboxUid)
        .then(() => {
            if (!state.currentFolderKey) {
                return dispatch("selectFolder", { folderKey: getters.my.INBOX.key, filter: state.messageFilter });
            }
        })
        .then(() => getters.my.folders.forEach(folder => dispatch("loadUnreadCount", folder.uid)))
        .then(() => {
            dispatch("mailboxes/all", { verb: [Verb.Read, Verb.Write, Verb.All], type: "mailboxacl" })
                .then(() => {
                    getters.mailshares.forEach(mailshare => dispatch("folders/all", mailshare.mailboxUid));
                })
                .then(() => {
                    return injector
                        .getProvider("MailboxesPersistence")
                        .get()
                        .getMailboxConfig(getters.my.uid);
                })
                .then(mailboxConfig => {
                    commit("setMaxMessageSize", mailboxConfig.messageMaxSize);
                });
        });
}
