import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function $_move(context, { messageKeys, destinationKey }) {
    const messageKeysByFolder = ItemUri.urisByContainer(messageKeys);
    return Promise.all(
        Object.keys(messageKeysByFolder).map(folderKey =>
            moveByFolder(context, messageKeysByFolder[folderKey], folderKey, destinationKey)
        )
    );
}

function moveByFolder({ dispatch, commit, rootState }, messageKeys, sourceKey, destinationKey) {
    const destination = rootState.mail.folders[destinationKey];
    const source = rootState.mail.folders[sourceKey];
    if (destination.mailbox === source.mailbox) {
        return moveInsideSameMailbox(destination.mailbox, destination.id, source.id, messageKeys, dispatch);
    } else {
        return moveToDifferentMailbox(messageKeys, sourceKey, destination.uid, dispatch, commit);
    }
}

function moveInsideSameMailbox(destinationMailbox, destinationInternalId, sourceInternalId, messageKeys, dispatch) {
    dispatch("_removeMessages", messageKeys);
    return ServiceLocator.getProvider("MailboxFoldersPersistence")
        .get(destinationMailbox)
        .importItems(destinationInternalId, {
            mailboxFolderId: sourceInternalId,
            ids: messageKeys.map(key => {
                return { id: ItemUri.item(key) };
            }),
            expectedIds: undefined,
            deleteFromSource: true
        });
}

function moveToDifferentMailbox(messageKeys, sourceUid, destinationUid, dispatch) {
    dispatch("_removeMessages", messageKeys);
    const messageIds = messageKeys.map(key => ItemUri.item(key));
    return ServiceLocator.getProvider("ItemsTransferPersistence").get(sourceUid, destinationUid).move(messageIds);
}
