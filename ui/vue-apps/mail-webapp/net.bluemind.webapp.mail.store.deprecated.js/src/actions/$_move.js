import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function $_move(context, { messageKeys, destinationKey }) {
    const messageKeysByFolder = ItemUri.urisByContainer(messageKeys);
    return Promise.all(
        Object.keys(messageKeysByFolder).map(folder =>
            moveByFolder(context, messageKeysByFolder[folder], folder, destinationKey)
        )
    );
}

function moveByFolder({ getters, dispatch, commit }, messageKeys, sourceUid, destinationKey) {
    const destination = getters["folders/getFolderByKey"](destinationKey);
    const source = getters["folders/getFolderByKey"](ItemUri.encode(sourceUid, getters.currentMailbox.mailboxUid));
    const destinationMailbox = ItemUri.container(destinationKey);
    if (getters.currentMailbox.mailboxUid === destinationMailbox) {
        return moveInsideSameMailbox(
            destinationMailbox,
            destination.internalId,
            source.internalId,
            messageKeys,
            dispatch
        );
    } else {
        return moveToDifferentMailbox(messageKeys, sourceUid, destination.uid, dispatch, commit);
    }
}

function moveInsideSameMailbox(destinationMailbox, destinationInternalId, sourceInternalId, messageKeys, dispatch) {
    return ServiceLocator.getProvider("MailboxFoldersPersistence")
        .get(destinationMailbox)
        .importItems(destinationInternalId, {
            mailboxFolderId: sourceInternalId,
            ids: messageKeys.map(key => {
                return { id: ItemUri.item(key) };
            }),
            expectedIds: undefined,
            deleteFromSource: true
        })
        .then(() => dispatch("_removeMessages", messageKeys));
}

function moveToDifferentMailbox(messageKeys, sourceUid, destinationUid, dispatch) {
    const messageIds = messageKeys.map(key => ItemUri.item(key));
    return ServiceLocator.getProvider("ItemsTransferPersistence")
        .get(sourceUid, destinationUid)
        .move(messageIds)
        .then(() => dispatch("_removeMessages", messageKeys));
}
