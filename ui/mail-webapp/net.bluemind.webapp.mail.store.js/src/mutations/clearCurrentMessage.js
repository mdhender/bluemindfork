import { MailboxItemsStore } from "@bluemind/backend.mail.store";

export function clearCurrentMessage(state) {
    MailboxItemsStore.mutations.clearParts(state.messages);
    state.currentMessageKey = undefined;
    state.currentMessageParts.attachments.splice();
    state.currentMessageParts.inlines.splice();
}
