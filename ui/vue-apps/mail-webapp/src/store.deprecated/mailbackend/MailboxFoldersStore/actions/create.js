import UUIDGenerator from "@bluemind/uuid";

export async function create({ rootState, dispatch, commit }, { name, parent, mailboxUid }) {
    const mailbox = rootState.mail.mailboxes[mailboxUid];
    const key = UUIDGenerator.generate();
    await dispatch("mail/CREATE_FOLDER", { key, name, parent, mailbox }, { root: true });
    const folder = rootState.mail.folders[key];
    commit("mail/REMOVE_FOLDER", key, { root: true });
    folder.key = folder.uid;
    commit("mail/ADD_FOLDER", folder, { root: true });
    return folder.uid;
}
