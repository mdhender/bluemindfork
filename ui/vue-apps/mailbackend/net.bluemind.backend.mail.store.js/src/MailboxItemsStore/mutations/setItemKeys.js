import ItemUri from "@bluemind/item-uri";

export function setItemKeys(state, { ids, folderUid }) {
    state.itemKeys = ids.map(id => ItemUri.encode(id, folderUid));
}
