import UUIDGenerator from "@bluemind/uuid";
import { CREATE_FOLDER, REMOVE_FOLDER, ADD_FOLDER } from "@bluemind/webapp.mail.store";

export async function create({ rootState, dispatch, commit }, { name, parent, mailboxUid }) {
    const mailbox = rootState.mail.mailboxes[mailboxUid];
    const key = UUIDGenerator.generate();
    await dispatch(CREATE_FOLDER, { key, name, parent, mailbox }, { root: true });
    const folder = rootState.mail.folders[key];
    commit(REMOVE_FOLDER, key, { root: true });
    folder.key = folder.uid;
    commit(ADD_FOLDER, folder, { root: true });
    return folder.uid;
}
