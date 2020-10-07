import actionTypes from "../../store/actionTypes";

export async function $_Vuebus_mailboxRecordsChanged({ dispatch, rootGetters, rootState }) {
    const activeFolderKey = rootState.mail.activeFolder;
    if (!activeFolderKey) {
        console.error("this action must not be called if no activeFolderKey is set.");
        return;
    }
    const folder = rootState.mail.folders[activeFolderKey];
    const conversationsEnabled = rootState.session.userSettings.mail_thread === "true";
    await dispatch("mail/" + actionTypes.REFRESH_MESSAGE_LIST_KEYS, { folder, conversationsEnabled }, { root: true });

    const sorted = rootState.mail.messageList.messageKeys.filter(key => rootGetters["mail/isLoaded"](key));
    await dispatch("mail/" + actionTypes.FETCH_MESSAGE_METADATA, { messageKeys: sorted.slice(0, 200) }, { root: true });
}
