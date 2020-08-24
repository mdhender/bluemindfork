import { inject } from "@bluemind/inject";

async function getAllFolders(mailbox) {
    return await apiClient(mailbox).all();
}

async function createNewFolder(mailbox, folder) {
    return await apiClient(mailbox).createBasic(folder.value);
}
async function deleteFolder(mailbox, folder) {
    await apiClient(mailbox).deepDelete(folder.id);
}
async function updateFolder(mailbox, folder) {
    await apiClient(mailbox).updateById(folder.internalId, folder.value);
}

function apiClient({ uid }) {
    return inject("MailboxFoldersPersistence", uid);
}

export default {
    getAllFolders,
    createNewFolder,
    deleteFolder,
    updateFolder
};
