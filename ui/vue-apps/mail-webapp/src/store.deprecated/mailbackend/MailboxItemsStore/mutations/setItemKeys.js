import ItemUri from "@bluemind/item-uri";

export function setItemKeysByIdsFolderUid(state, { ids, folderUid }) {
    setItemKeys(
        state,
        ids.map(id => ItemUri.encode(id, folderUid))
    );
}

export function setItemKeys(state, itemKeys) {
    state.itemKeys = itemKeys;
}
