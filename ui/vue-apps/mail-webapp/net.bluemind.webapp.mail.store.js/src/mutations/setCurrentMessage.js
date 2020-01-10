import { clearCurrentMessage } from "./clearCurrentMessage";

export function setCurrentMessage(state, key) {
    clearCurrentMessage(state);
    state.currentMessageKey = key;
}
