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

function moveToDifferentMailbox(messageKeys, sourceUid, destinationUid, dispatch, commit) {
    return dispatch("$_getIfNotPresent", messageKeys).then(messages =>
        Promise.all(messages.map(m => fetchAndMove(sourceUid, m, destinationUid, dispatch, commit)))
    );
}

function fetchAndMove(sourceUid, message, destinationUid, dispatch, commit) {
    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(sourceUid)
        .fetchComplete(message.imapUid)
        .then(eml =>
            ServiceLocator.getProvider("MailboxItemsPersistence")
                .get(destinationUid)
                .uploadPart(eml)
        )
        .then(part =>
            ServiceLocator.getProvider("MailboxItemsPersistence")
                .get(destinationUid)
                .create({
                    body: { structure: { mime: "message/rfc822", address: part } }
                })
        )
        .then(() => dispatch("messages/remove", message.key))
        .then(() => commit("deleteSelectedMessageKey", message.key));
}
