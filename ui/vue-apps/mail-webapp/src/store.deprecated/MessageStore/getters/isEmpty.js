import Message from "../../mailbackend/MailboxItemsStore/Message";

export function isEmpty(state) {
    return new Message(null, state).isEmpty();
}
