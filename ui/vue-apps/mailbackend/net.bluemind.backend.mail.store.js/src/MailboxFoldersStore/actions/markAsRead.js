import injector from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export async function markAsRead({ getters }, folderKey) {
    const mailboxUid = ItemUri.container(folderKey);
    const folder = getters.getFolderByKey(folderKey);
    const service = injector.getProvider("MailboxFoldersPersistence").get(mailboxUid);
    await service.markFolderAsRead(folder.internalId);
}
