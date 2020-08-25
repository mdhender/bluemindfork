import Vue from "vue";

export function removeItems(state, messageKeys) {
    messageKeys.forEach(messageKey => {
        state.itemKeys.splice(state.itemKeys.indexOf(messageKey), 1);
        Vue.delete(state.items, messageKey);
        if (state.itemsParts[messageKey]) {
            state.itemsParts[messageKey].forEach(partKey => Vue.delete(state.partContents, partKey));
        }
        Vue.delete(state.itemsParts, messageKey);
    });
}
