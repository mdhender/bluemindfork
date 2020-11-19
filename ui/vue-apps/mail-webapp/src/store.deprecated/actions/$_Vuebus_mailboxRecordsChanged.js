import { FETCH_MESSAGE_METADATA, REFRESH_MESSAGE_LIST_KEYS } from "~actions";

export async function $_Vuebus_mailboxRecordsChanged({ dispatch, rootState }) {
    const activeFolderKey = rootState.mail.activeFolder;
    if (!activeFolderKey) {
        console.error("this action must not be called if no activeFolderKey is set.");
        return;
    }
    const folder = rootState.mail.folders[activeFolderKey];
    const conversationsEnabled = rootState.session.userSettings.mail_thread === "true";
    await dispatch("mail/" + REFRESH_MESSAGE_LIST_KEYS, { folder, conversationsEnabled }, { root: true });

    const sorted = rootState.mail.messageList.messageKeys.slice(0, 200).map(key => rootState.mail.messages[key]);
    await dispatch("mail/" + FETCH_MESSAGE_METADATA, sorted, { root: true });
}
