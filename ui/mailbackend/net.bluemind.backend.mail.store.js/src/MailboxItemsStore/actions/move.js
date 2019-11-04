import ServiceLocator from "@bluemind/inject";

export function move({ commit }, { sourceId, destinationId, messageId }) {
    return ServiceLocator.getProvider("MailboxFoldersPersistence")
        .get()
        .importItems(destinationId, {
            mailboxFolderId: sourceId,
            ids: [{ id: messageId }],
            expectedIds: undefined,
            deleteFromSource: true
        })
        .then(() => {
            commit("removeItems", [messageId]);
        });
}
