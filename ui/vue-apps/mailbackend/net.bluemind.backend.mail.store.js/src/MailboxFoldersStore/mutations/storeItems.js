import ItemUri from "@bluemind/item-uri";
import Vue from "vue";
import sortedIndexBy from "lodash.sortedindexby";

export function storeItems(state, { items, mailboxUid }) {
    const cmp = compare.bind(null, state, mailboxUid);
    if (!state.itemsByContainer[mailboxUid]) {
        Vue.set(state.itemsByContainer, mailboxUid, []);
    }
    items.forEach(item => {
        const key = ItemUri.encode(item.uid, mailboxUid);
        Vue.set(state.items, key, item);
        if (!state.itemKeys.includes(key)) {
            const index = sortedIndexBy(state.itemKeys, key, cmp);
            state.itemKeys.splice(index, 0, key);
        }
        if (!state.itemsByContainer[mailboxUid].includes(key)) {
            state.itemsByContainer[mailboxUid].push(key);
        }
    });
}

function compare(state, mailboxUid, key) {
    const item = state.items[key];
    return item.value.mailboxUid + "." + item.value.fullName;
}
