import { Verb } from "@bluemind/core.container.api";

export async function bootstrap({ dispatch, state, getters, commit }, userUid) {
    commit("setUserUid", userUid);

    try {
        await dispatch("mailboxes/all", { verb: [Verb.Read, Verb.Write, Verb.All], type: "mailboxacl" });
        await dispatch("folders/all", getters.my.mailboxUid);
        if (!state.currentFolderKey) {
            await dispatch("loadMessageList", { folder: getters.my.INBOX.key, filter: state.messageFilter });
        }
        await getters.my.folders.forEach(folder => dispatch("loadUnreadCount", folder.uid));
        await Promise.all(getters.mailshares.map(mailshare => dispatch("folders/all", mailshare.mailboxUid)));
        await dispatch("loadUserSettings");
        await dispatch("loadMailboxConfig");
    } catch (e) {
        console.log("Failure occurred but bootstrap must not fail", e);
    }
}
