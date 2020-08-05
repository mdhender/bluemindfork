import { Verb } from "@bluemind/core.container.api";

export async function bootstrap({ dispatch, commit, rootGetters }, userUid) {
    commit("setUserUid", userUid);

    try {
        await dispatch("mailboxes/all", { verb: [Verb.Read, Verb.Write, Verb.All], type: "mailboxacl" });
        await dispatch("folders/all", rootGetters["mail/MY_MAILBOX_KEY"]);
        rootGetters["mail/MY_MAILBOX_FOLDERS"].forEach(folderKey => dispatch("loadUnreadCount", folderKey));
        await Promise.all(
            rootGetters["mail/MAILSHARE_KEYS"].map(mailshareKey => dispatch("folders/all", mailshareKey))
        );
        dispatch("loadUserSettings");
        dispatch("loadMailboxConfig");
    } catch (e) {
        console.log("Failure occurred but bootstrap must not fail", e);
    }
}
