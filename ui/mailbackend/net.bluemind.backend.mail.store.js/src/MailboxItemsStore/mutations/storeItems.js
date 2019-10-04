import Vue from "vue";

export function storeItems(state, items) {
    items.forEach(item => {
        Vue.set(state.items, item.internalId, item);
    });
}
