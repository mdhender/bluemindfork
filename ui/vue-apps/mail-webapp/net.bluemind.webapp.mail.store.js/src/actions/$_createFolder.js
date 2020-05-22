import ItemUri from "@bluemind/item-uri";

export function $_createFolder({ dispatch, getters }, { folder, mailboxUid }) {
    let parent = getNearestFolder(folder, getters);
    let hierarchy = folder.value.fullName;
    if (parent !== null) {
        hierarchy = hierarchy.replace(parent.value.fullName, "");
    }
    return hierarchy
        .split("/")
        .filter(Boolean)
        .reduce(
            (promise, folder) =>
                promise.then(key =>
                    dispatch("folders/create", {
                        name: folder,
                        parentUid: key ? ItemUri.item(key) : null,
                        mailboxUid
                    })
                ),
            Promise.resolve(parent && parent.key)
        );
}

function get(folders, hierarchy, parent) {
    const name = hierarchy[0];
    const folder = folders.find(folder => equal(folder, { parentUid: parent && parent.uid, name }));
    if (!folder) {
        return parent;
    }
    hierarchy.shift();
    if (hierarchy.length > 0) {
        return get(folders, hierarchy, folder);
    }
    return folder;
}

function equal(folder, { name, parentUid }) {
    return folder.value.parentUid === parentUid && name.toLowerCase() === folder.value.name.toLowerCase();
}

function getNearestFolder(folder, getters) {
    if (folder.key) {
        return getters["folders/getFolderByKey"](folder.key);
    } else {
        const hierarchy = folder.value.fullName.split("/").filter(Boolean);
        return get(getters["folders/folders"], hierarchy, null);
    }
}
