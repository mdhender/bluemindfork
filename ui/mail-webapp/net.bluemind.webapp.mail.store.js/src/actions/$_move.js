import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function $_move({ getters, commit }, { messageKey, destinationKey }) {
    const [messageId, sourceUid] = ItemUri.decode(messageKey);
    const destination = getters["folders/getFolderByKey"](destinationKey);
    const source = getters["folders/getFolderByKey"](ItemUri.encode(sourceUid, getters.my.mailboxUid));

    return ServiceLocator.getProvider("MailboxFoldersPersistence")
        .get(ItemUri.container(destinationKey))
        .importItems(destination.internalId, {
            mailboxFolderId: source.internalId,
            ids: [{ id: messageId }],
            expectedIds: undefined,
            deleteFromSource: true
        })
        .then(() => commit("messages/removeItems", [messageKey]));
}
