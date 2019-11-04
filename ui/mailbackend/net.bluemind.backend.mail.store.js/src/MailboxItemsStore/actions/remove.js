import ServiceLocator from "@bluemind/inject";

export function remove({ commit }, { messageId, folderUid }) {
    return ServiceLocator.getProvider("MailboxItemsPersistence").get(folderUid)
        .deleteById(messageId)
        .then(() => {
            commit("removeItems", [messageId]);
        });
}
