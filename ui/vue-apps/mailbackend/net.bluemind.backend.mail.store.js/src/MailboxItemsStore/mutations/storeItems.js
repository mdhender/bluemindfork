import Vue from "vue";
import ItemUri from "@bluemind/item-uri";

export function storeItems(state, { items, folderUid }) {
    items.forEach(item => {
        const key = ItemUri.encode(item.internalId, folderUid);
        if (!state.itemKeys.includes(key)) {
            state.itemKeys.push(key);
        }
        Vue.set(state.items, key, item);
        if (!state.itemsParts[key]) {
            Vue.set(state.itemsParts, key, []);
        }
    });
}
