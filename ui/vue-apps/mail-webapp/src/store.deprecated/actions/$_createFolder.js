import UUIDGenerator from "@bluemind/uuid";

export function $_createFolder(context, { folder, mailboxUid }) {
    if (folder.key) {
        return folder.key;
    }
    let parent = getNearestFolder(folder, context.rootState);
    let hierarchy = folder.path;
    if (parent !== null) {
        hierarchy = hierarchy.replace(parent.path, "");
    }
    return hierarchy
        .split("/")
        .filter(Boolean)
        .reduce(
            (promise, folder) => promise.then(key => createFolder(folder, key, mailboxUid, context)),
            Promise.resolve(parent && parent.key)
        );
}

function get(folders, hierarchy, parent) {
    const name = hierarchy[0];
    const folder = folders.find(folder => equal(folder, { parent: parent && parent.key, name }));
    if (!folder) {
        return parent;
    }
    hierarchy.shift();
    if (hierarchy.length > 0) {
        return get(folders, hierarchy, folder);
    }
    return folder;
}

function equal(folder, { name, parent }) {
    return folder.parent === parent && name.toLowerCase() === folder.name.toLowerCase();
}

function getNearestFolder(folder, rootState) {
    const hierarchy = folder.path.split("/").filter(Boolean);
    return get(Object.values(rootState.mail.folders), hierarchy, null);
}

async function createFolder(name, parentKey, mailboxUid, context) {
    const mailbox = context.rootState.mail.mailboxes[mailboxUid];
    const key = UUIDGenerator.generate();
    await context.dispatch("mail/CREATE_FOLDER", { key, name, parent: parentKey, mailbox }, { root: true });
    const folder = context.rootState.mail.folders[key];
    context.commit("mail/REMOVE_FOLDER", key, { root: true });
    folder.key = folder.uid;
    context.commit("mail/ADD_FOLDER", folder, { root: true });
    return folder.uid;
}
