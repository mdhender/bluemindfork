import ItemUri from "@bluemind/item-uri";
import Vue from "vue";

export function storeItems(state, { items, mailboxUid }) {
    if (!state.itemsByContainer[mailboxUid]) {
        Vue.set(state.itemsByContainer, mailboxUid, []);
    }
    items.forEach(item => {
        const key = ItemUri.encode(item.uid, mailboxUid);
        if (!state.itemKeys.includes(key)) {
            state.itemKeys.push(key);
        }
        if (!state.itemsByContainer[mailboxUid].includes(key)) {
            state.itemsByContainer[mailboxUid].push(key);
        }
        Vue.set(state.items, key, item);
    });
}
