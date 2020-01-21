import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function $_move({ getters, commit, dispatch }, { messageKey, destinationKey }) {
    const [messageId, sourceUid] = ItemUri.decode(messageKey);
    const destination = getters["folders/getFolderByKey"](destinationKey);
    const source = getters["folders/getFolderByKey"](ItemUri.encode(sourceUid, getters.currentMailbox.mailboxUid));
    const destinationMailbox = ItemUri.container(destinationKey);
    if (getters.currentMailbox.mailboxUid === destinationMailbox) {
        return ServiceLocator.getProvider("MailboxFoldersPersistence")
            .get(destinationMailbox)
            .importItems(destination.internalId, {
                mailboxFolderId: source.internalId,
                ids: [{ id: messageId }],
                expectedIds: undefined,
                deleteFromSource: true
            })
            .then(() => commit("messages/removeItems", [messageKey]));
    } else {
        return dispatch("$_getIfNotPresent", messageKey)
            .then(message => {
                return ServiceLocator.getProvider("MailboxItemsPersistence")
                    .get(sourceUid)
                    .fetchComplete(message.imapUid);
            })
            .then(eml =>
                ServiceLocator.getProvider("MailboxItemsPersistence")
                    .get(destination.uid)
                    .uploadPart(eml)
            )
            .then(part =>
                ServiceLocator.getProvider("MailboxItemsPersistence")
                    .get(destination.uid)
                    .create({
                        body: { structure: { mime: "message/rfc822", address: part } }
                    })
            )
            .then(() => dispatch("messages/remove", messageKey));
    }
}
