import injector from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export async function rename({ getters, commit }, { folderKey, newFolderName }) {
    const mailboxUid = ItemUri.container(folderKey);
    const oldFolder = getters.getFolderByKey(folderKey);
    const newFolder = computeNewFolder(oldFolder, newFolderName);
    replaceItem(commit, folderKey, newFolder, mailboxUid);
    try {
        await injector
            .getProvider("MailboxFoldersPersistence")
            .get(mailboxUid)
            .updateById(newFolder.internalId, newFolder.value);
    } catch (e) {
        replaceItem(commit, folderKey, oldFolder, mailboxUid);
        throw e;
    }
}

function replaceItem(commit, folderKey, folder, mailboxUid) {
    commit("removeItems", [folderKey]);
    commit("storeItems", { items: [folder], mailboxUid });
}

function computeNewFolder(old, newName) {
    const newFolder = JSON.parse(JSON.stringify(old));
    newFolder.value.name = newName;
    newFolder.displayName = newName;
    if (old.value.fullName.includes("/")) {
        newFolder.value.fullName =
            newFolder.value.fullName.substring(0, newFolder.value.fullName.lastIndexOf("/") + 1) + newName;
    } else {
        newFolder.value.fullName = newName;
    }

    return newFolder;
}
