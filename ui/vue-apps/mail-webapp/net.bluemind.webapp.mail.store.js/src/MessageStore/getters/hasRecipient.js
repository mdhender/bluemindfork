import { Message } from "@bluemind/backend.mail.store";

export function hasRecipient(state) {
    return new Message(null, state).hasRecipient();
}
