import { inject } from "@bluemind/inject";
import { ItemFlag } from "@bluemind/core.container.api";

async function getAllFolders(mailbox) {
    return await apiClient(mailbox.remoteRef).all();
}

async function createNewFolder(mailbox, remoteFolder) {
    return await apiClient(mailbox.remoteRef).createBasic(remoteFolder.value);
}
async function deleteFolder(mailbox, folder) {
    await apiClient(mailbox.remoteRef).deepDelete(folder.remoteRef.internalId);
}
async function updateFolder(mailbox, remoteFolder) {
    await apiClient(mailbox.remoteRef).updateById(remoteFolder.internalId, remoteFolder.value);
}
async function markAsRead(mailbox, folder) {
    return await apiClient(mailbox.remoteRef).markFolderAsRead(folder.remoteRef.internalId);
}
async function emptyFolder(mailbox, folder, deep) {
    if (deep) {
        return await apiClient(mailbox.remoteRef).emptyFolder(folder.remoteRef.internalId);
    } else {
        return await apiClient(mailbox.remoteRef).removeMessages(folder.remoteRef.internalId);
    }
}
async function unreadCount(folder) {
    return apiItems(folder).count({ must: [], mustNot: [ItemFlag.Deleted, ItemFlag.Seen] });
}

function apiClient({ uid }) {
    return inject("MailboxFoldersPersistence", uid);
}
function apiItems({ remoteRef: { uid } }) {
    return inject("MailboxItemsPersistence", uid);
}

export default {
    getAllFolders,
    createNewFolder,
    deleteFolder,
    emptyFolder,
    markAsRead,
    updateFolder,
    unreadCount
};
