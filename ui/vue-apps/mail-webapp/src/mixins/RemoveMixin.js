import { CONVERSATIONS_ACTIVATED, IS_CURRENT_CONVERSATION, MY_TRASH, NEXT_CONVERSATION } from "~/getters";
import {
    MOVE_CONVERSATIONS,
    MOVE_CONVERSATION_MESSAGES,
    REMOVE_CONVERSATIONS,
    REMOVE_CONVERSATION_MESSAGES
} from "~/actions";
import FormattedDateMixin from "./FormattedDateMixin";
import SelectionMixin from "./SelectionMixin";
import { conversationMustBeRemoved } from "~/model/conversations";
import { MailRoutesMixin } from "~/mixins";
import { mapGetters } from "vuex";

export default {
    mixins: [FormattedDateMixin, MailRoutesMixin, SelectionMixin],
    computed: {
        ...mapGetters("mail", { $_RemoveMixin_CONVERSATIONS_ACTIVATED: CONVERSATIONS_ACTIVATED })
    },
    methods: {
        MOVE_CONVERSATIONS_TO_TRASH: navigateConversations(async function (conversations) {
            const trash = this.$store.getters[`mail/${MY_TRASH}`];

            if (conversations.some(conversation => conversation.folderRef.key !== trash.key)) {
                this.$store.dispatch(`mail/${MOVE_CONVERSATIONS}`, { conversations, folder: trash });
                return true;
            } else {
                return await this.REMOVE_CONVERSATIONS(conversations);
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
                    cancelVariant: "outline-secondary",
                    cancelTitle: this.$t("common.cancel"),
                    centered: true,
                    hideHeaderClose: false,
                    autoFocusButton: "ok"
                }
            );
            if (confirm) {
                this.$store.dispatch(`mail/${REMOVE_CONVERSATIONS}`, { conversations });
            }
            return confirm;
        }),
        MOVE_MESSAGES_TO_TRASH: navigate(async function (conversation, messages) {
            const trash = this.$store.getters[`mail/${MY_TRASH}`];

            if (messages.some(message => message.folderRef.key !== trash.key)) {
                this.$store.dispatch(`mail/${MOVE_CONVERSATION_MESSAGES}`, { conversation, messages, folder: trash });
                return true;
            } else {
                return await this.REMOVE_MESSAGES({ conversation, messages });
            }
        }),
        REMOVE_MESSAGES: navigate(async function (conversation, messages) {
            const confirm = await this.$bvModal.msgBoxConfirm(
                this.$tc("mail.actions.purge.modal.content", messages.length, messages[0]),
                {
                    title: this.$tc("mail.actions.purge.modal.title", messages.length),
                    okTitle: this.$t("common.delete"),
                    cancelVariant: "outline-secondary",
                    cancelTitle: this.$t("common.cancel"),
                    centered: true,
                    hideHeaderClose: false,
                    autoFocusButton: "ok"
                }
            );
            if (confirm) {
                await this.$store.dispatch(`mail/${REMOVE_CONVERSATION_MESSAGES}`, { conversation, messages });
            }
            return confirm;
        }),
        async REMOVE_DRAFT(conversation, draft) {
            const formattedDate = this.formatMessageDate(draft);
            const textKey = formattedDate.date
                ? "mail.actions.purge.draft.modal.content"
                : "mail.actions.purge.draft.modal.content.time";
            const confirm = await this.$bvModal.msgBoxConfirm(this.$t(textKey, { ...draft, ...formattedDate }), {
                title: this.$t("mail.actions.purge.draft.modal.title"),
                okTitle: this.$t("common.delete"),
                cancelVariant: "outline-secondary",
                cancelTitle: this.$t("common.cancel"),
                centered: true,
                hideHeaderClose: false,
                autoFocusButton: "ok"
            });
            if (confirm) {
                await this.$store.dispatch(`mail/${REMOVE_CONVERSATION_MESSAGES}`, { conversation, messages: [draft] });
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
    return async function (conversation, messages) {
        const next = this.$store.getters["mail/" + NEXT_CONVERSATION]([conversation]);
        const isCurrentConversation = this.$store.getters["mail/" + IS_CURRENT_CONVERSATION](conversation);
        messages = Array.isArray(messages) ? [...messages] : [messages];
        const confirm = await action.call(this, conversation, messages);
        if (
            confirm &&
            isCurrentConversation &&
            conversationMustBeRemoved(this.$store.state.mail.conversations, conversation, messages)
        ) {
            this.navigateTo(next, conversation.folderRef);
        }
    };
}
