//FIXME: A action which return something ?
import ServiceLocator from "@bluemind/inject";

export function create({ commit }, { name, parentUid }) {
    const service = ServiceLocator.getProvider("MailboxFoldersPersistance").get();
    let newFolderItemIdentifier;
    return service
        .createBasic({
            name: name,
            fullName: name,
            parentUid: parentUid,
            deleted: false
        })
        .then(itemIdentifier => {
            newFolderItemIdentifier = itemIdentifier;
            return service.getComplete(itemIdentifier.uid);
        })
        .then(mailboxFolder => {
            commit("add", mailboxFolder);
            return newFolderItemIdentifier;
        });
}
