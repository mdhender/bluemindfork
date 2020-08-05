import injector from "@bluemind/inject";

export async function markAsRead({ rootState }, folderKey) {
    const folder = rootState.mail.folders[folderKey];
    const service = injector.getProvider("MailboxFoldersPersistence").get(folder.mailbox);
    await service.markFolderAsRead(folder.id);
}
