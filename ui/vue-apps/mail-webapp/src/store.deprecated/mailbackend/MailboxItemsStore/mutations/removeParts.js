import Vue from "vue";

export function removeParts(state, messageKeys) {
    messageKeys.forEach(messageKey => {
        if (state.itemsParts[messageKey]) {
            state.itemsParts[messageKey].forEach(partKey => Vue.delete(state.partContents, partKey));
        }
        Vue.delete(state.itemsParts, messageKey);
    });
}
