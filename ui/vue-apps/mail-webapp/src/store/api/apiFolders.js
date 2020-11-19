import { inject } from "@bluemind/inject";

async function getAllFolders(mailbox) {
    return await apiClient(mailbox).all();
}

async function createNewFolder(mailbox, remoteFolder) {
    return await apiClient(mailbox).createBasic(remoteFolder.value);
}
async function deleteFolder(mailbox, folder) {
    await apiClient(mailbox).deepDelete(folder.remoteRef.internalId);
}
async function updateFolder(mailbox, remoteFolder) {
    await apiClient(mailbox).updateById(remoteFolder.internalId, remoteFolder.value);
}
async function markAsRead(mailbox, folder) {
    return await apiClient(mailbox).markFolderAsRead(folder.remoteRef.internalId);
}
function apiClient({ remoteRef: { uid } }) {
    return inject("MailboxFoldersPersistence", uid);
}

export default {
    getAllFolders,
    createNewFolder,
    deleteFolder,
    markAsRead,
    updateFolder
};
