import Vue from "vue";
import ItemUri from "@bluemind/item-uri";

export function removeItems(state, folderKeys) {
    folderKeys.forEach(key => {
        state.itemKeys.splice(state.itemKeys.indexOf(key), 1);
        Vue.delete(state.items, key);
        const mailboxUid = ItemUri.container(key);
        state.itemsByContainer[mailboxUid].splice(state.itemsByContainer[mailboxUid].indexOf(key), 1);
    });
}
