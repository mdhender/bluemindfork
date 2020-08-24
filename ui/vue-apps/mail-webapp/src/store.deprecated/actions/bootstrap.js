import { FETCH_FOLDERS } from "../../store/folders/actions";

export async function bootstrap({ dispatch, commit, rootGetters, rootState }, userUid) {
    commit("setUserUid", userUid);
    await dispatch("mail/FETCH_MAILBOXES", null, { root: true });
    await dispatch("mail/" + FETCH_FOLDERS, rootGetters["mail/MY_MAILBOX"], { root: true });
    rootGetters["mail/MY_MAILBOX_FOLDERS"].forEach(folderKey => dispatch("loadUnreadCount", folderKey));
    await Promise.all(
        rootGetters["mail/MAILSHARE_KEYS"].map(mailshareKey =>
            dispatch("mail/" + FETCH_FOLDERS, rootState.mail.mailboxes[mailshareKey], { root: true })
        )
    );
    dispatch("loadMailboxConfig");
}
