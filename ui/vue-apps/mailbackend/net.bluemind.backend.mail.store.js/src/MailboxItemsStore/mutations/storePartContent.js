import Vue from "vue";
import { PartKey } from "../PartKey";

export function storePartContent(state, { messageKey, address, content }) {
    const key = PartKey.encode(address, messageKey);
    if (!state.itemsParts[messageKey].includes(key)) {
        state.itemsParts[messageKey].push(key);
    }
    Vue.set(state.partContents, key, content);
}
