import { Message } from "@bluemind/backend.mail.store";

export function isEmpty(state) {
    return new Message(null, state).isEmpty();
}
