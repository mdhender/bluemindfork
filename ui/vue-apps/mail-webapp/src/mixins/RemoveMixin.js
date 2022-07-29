import { mapGetters } from "vuex";
import { conversationUtils } from "@bluemind/mail";
import {
    CONVERSATIONS_ACTIVATED,
    CURRENT_MAILBOX,
    IS_CURRENT_CONVERSATION,
    MAILBOX_TRASH,
    NEXT_CONVERSATION
} from "~/getters";
import {
    MOVE_CONVERSATIONS,
    MOVE_CONVERSATION_MESSAGES,
    REMOVE_CONVERSATIONS,
    REMOVE_CONVERSATION_MESSAGES,
    REMOVE_MESSAGES,
    MOVE_MESSAGES
} from "~/actions";
import FormattedDateMixin from "./FormattedDateMixin";
import SelectionMixin from "./SelectionMixin";
import MailRoutesMixin from "./MailRoutesMixin";

const { conversationMustBeRemoved } = conversationUtils;

export default {
    mixins: [FormattedDateMixin, MailRoutesMixin, SelectionMixin],
    computed: {
        ...mapGetters("mail", {
            $_RemoveMixin_CONVERSATIONS_ACTIVATED: CONVERSATIONS_ACTIVATED
        })
    },
    methods: {
        MOVE_CONVERSATIONS_TO_TRASH: navigateConversations(function (conversations) {
            const trash = getConversationsTrash(this.$store, conversations);
            if (conversations.some(conversation => conversation.folderRef.key !== trash.key)) {
                this.$store.dispatch(`mail/${MOVE_CONVERSATIONS}`, {
                    conversations,
                    conversationsActivated: this.$store.getters[`mail/${CONVERSATIONS_ACTIVATED}`],
                    folder: trash,
                    mailbox: this.$store.getters[`mail/${CURRENT_MAILBOX}`]
                });
                return true;
            } else {
                return this.REMOVE_CONVERSATIONS(conversations);
            }
        }),
        REMOVE_CONVERSATIONS: navigateConversations(async function (conversations) {
            const textKey = this.$_RemoveMixin_CONVERSATIONS_ACTIVATED
                ? "mail.actions.purge.conversations.modal.content"
                : "mail.actions.purge.modal.content";
            const titleKey = this.$_RemoveMixin_CONVERSATIONS_ACTIVATED
                ? "mail.actions.purge.conversations.modal.title"
                : "mail.actions.purge.modal.title";
            const confirm = await this.$bvModal.msgBoxConfirm(
                this.$tc(textKey, conversations.length, conversations[0]),
                {
                    title: this.$tc(titleKey, conversations.length),
                    okTitle: this.$t("common.delete"),
                    cancelTitle: this.$t("common.cancel"),
                    okVariant: "fill-accent",
                    cancelVariant: "text",
                    centered: true,
                    hideHeaderClose: false,
                    autoFocusButton: "ok"
                }
            );
            if (confirm) {
                const conversationsActivated = this.$store.getters[`mail/${CONVERSATIONS_ACTIVATED}`];
                this.$store.dispatch(`mail/${REMOVE_CONVERSATIONS}`, {
                    conversations,
                    conversationsActivated,
                    mailbox: this.$store.getters[`mail/${CURRENT_MAILBOX}`]
                });
            }
            return confirm;
        }),
        MOVE_MESSAGES_TO_TRASH: navigate(function (messages, conversation) {
            const trash = getConversationsTrash(this.$store, messages);

            if (messages.some(message => message.folderRef.key !== trash.key)) {
                if (conversation) {
                    this.$store.dispatch(`mail/${MOVE_CONVERSATION_MESSAGES}`, {
                        conversation,
                        messages,
                        folder: trash
                    });
                } else {
                    this.$store.dispatch(`mail/${MOVE_MESSAGES}`, { messages, folder: trash });
                }
                return true;
            } else {
                return this.REMOVE_MESSAGES(messages, conversation);
            }
        }),
        REMOVE_MESSAGES: navigate(async function (messages, conversation) {
            const confirm = await this.$bvModal.msgBoxConfirm(
                this.$tc("mail.actions.purge.modal.content", messages.length, messages[0]),
                {
                    title: this.$tc("mail.actions.purge.modal.title", messages.length),
                    okTitle: this.$t("common.delete"),
                    cancelTitle: this.$t("common.cancel"),
                    okVariant: "fill-accent",
                    cancelVariant: "text",
                    centered: true,
                    hideHeaderClose: false,
                    autoFocusButton: "ok"
                }
            );
            if (confirm) {
                if (conversation) {
                    this.$store.dispatch(`mail/${REMOVE_CONVERSATION_MESSAGES}`, { conversation, messages });
                } else {
                    this.$store.dispatch(`mail/${REMOVE_MESSAGES}`, { messages });
                }
            }
            return confirm;
        }),
        async REMOVE_DRAFT(draft, conversation) {
            const formattedDate = this.formatMessageDate(draft);
            const textKey = formattedDate.date
                ? "mail.actions.purge.draft.modal.content"
                : "mail.actions.purge.draft.modal.content.time";
            const confirm = await this.$bvModal.msgBoxConfirm(this.$t(textKey, { ...draft, ...formattedDate }), {
                title: this.$t("mail.actions.purge.draft.modal.title"),
                okTitle: this.$t("common.delete"),
                cancelTitle: this.$t("common.cancel"),
                okVariant: "fill-accent",
                cancelVariant: "text",
                centered: true,
                hideHeaderClose: false,
                autoFocusButton: "ok"
            });
            if (confirm) {
                if (conversation) {
                    this.$store.dispatch(`mail/${REMOVE_CONVERSATION_MESSAGES}`, { conversation, messages: [draft] });
                } else {
                    return this.$store.dispatch(`mail/${REMOVE_MESSAGES}`, { messages: [draft] });
                }
            }
            return confirm;
        },
        moveToTrash() {
            return this.MOVE_CONVERSATIONS_TO_TRASH(this.selected);
        },
        remove() {
            return this.REMOVE_CONVERSATIONS(this.selected);
        }
    }
};

function getConversationsTrash(store, conversations) {
    // Here we assume that all messages are from the same mailbox.
    // If one day the multi-mailbox selection is implemented, there are 2 ways :
    // - Simple way : If messages are from different mailboxes then all message are moved to the user's trash
    // - Complex way : If messages are from different mailboxes then all message are moved to the mailbox's trash
    const conversation = conversations[0];
    const folder = store.state.mail.folders[conversation.folderRef.key];
    const mailbox = store.state.mail.mailboxes[folder.mailboxRef.key];
    return store.getters[`mail/${MAILBOX_TRASH}`](mailbox);
}

function navigateConversations(action) {
    return async function (conversations) {
        const next = this.$store.getters["mail/" + NEXT_CONVERSATION](conversations);
        const isCurrentConversation = this.$store.getters["mail/" + IS_CURRENT_CONVERSATION](conversations[0]);
        const confirm = await action.call(this, conversations);
        if (confirm && isCurrentConversation) {
            this.navigateTo(next, conversations[0].folderRef);
        }
    };
}

function navigate(action) {
    return async function (messages, conversation) {
        messages = Array.isArray(messages) ? [...messages] : [messages];
        if (conversation) {
            const next = this.$store.getters["mail/" + NEXT_CONVERSATION]([conversation]);
            const isCurrentConversation = this.$store.getters["mail/" + IS_CURRENT_CONVERSATION](conversation);
            const confirm = await action.call(this, messages, conversation);
            if (
                confirm &&
                isCurrentConversation &&
                conversationMustBeRemoved(this.$store.state.mail.conversations, conversation, messages)
            ) {
                this.navigateTo(next, conversation.folderRef);
            }
        } else if (await action.call(this, messages)) {
            this.$router.push({ name: "mail:home" });
        }
    };
}
