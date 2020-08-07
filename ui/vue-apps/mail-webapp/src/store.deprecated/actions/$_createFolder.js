export function $_createFolder({ dispatch, rootState }, { folder, mailboxUid }) {
    if (folder.key) {
        return folder.key;
    }
    let parent = getNearestFolder(folder, rootState);
    let hierarchy = folder.path;
    if (parent !== null) {
        hierarchy = hierarchy.replace(parent.path, "");
    }
    return hierarchy
        .split("/")
        .filter(Boolean)
        .reduce(
            (promise, folder) =>
                promise.then(key => dispatch("folders/create", { name: folder, parent: key, mailboxUid })),
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
