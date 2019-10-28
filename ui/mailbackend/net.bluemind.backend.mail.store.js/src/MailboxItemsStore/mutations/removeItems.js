import Vue from "vue";

export function removeItems(state, messageIds) {
    messageIds.forEach(messageId => {
        state.sortedIds.splice(state.sortedIds.indexOf(messageId), 1);
        Vue.delete(state.items, messageId);
        Vue.delete(state.parts, messageId);
    });
}
