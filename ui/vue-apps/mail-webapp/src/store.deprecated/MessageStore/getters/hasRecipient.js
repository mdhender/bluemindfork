import Message from "../../mailbackend/MailboxItemsStore/Message";

export function hasRecipient(state) {
    return new Message(null, state).hasRecipient();
}
