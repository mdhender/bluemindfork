import { clearCurrentMessage } from "./clearCurrentMessage";

export function setCurrentMessage(state, id) {
    clearCurrentMessage(state);
    state.currentMessageId = id;
}
