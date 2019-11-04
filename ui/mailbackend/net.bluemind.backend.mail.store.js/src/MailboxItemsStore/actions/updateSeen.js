import ServiceLocator from "@bluemind/inject";

export function updateSeen({ commit, rootGetters }, { folder, id, isSeen }) {
    folder = folder || rootGetters["backend.mail/folders/currentFolder"];
    const service = ServiceLocator.getProvider("MailboxItemsPersistence").get(folder);
    return service
        .updateSeens([{ itemId: id, seen: isSeen, mdnSent: false }])
        .then(() => commit("updateSeen", { id, isSeen }));
}
