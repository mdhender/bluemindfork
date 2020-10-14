import { mapActions, mapGetters } from "vuex";
import { IS_CURRENT_CONVERSATION, MY_TRASH, NEXT_CONVERSATION } from "~/getters";
import { MOVE_CONVERSATIONS_TO_TRASH, MOVE_MESSAGES_TO_TRASH, REMOVE_CONVERSATIONS, REMOVE_MESSAGES } from "~/actions";
import { conversationsOnly } from "~/model/conversations";
import FormattedDateMixin from "./FormattedDateMixin";
import SelectionMixin from "./SelectionMixin";

export default {
    mixins: [FormattedDateMixin, SelectionMixin],
    computed: {
        ...mapGetters("mail", { $_RemoveMixin_trash: MY_TRASH })
    },
    methods: {
        ...mapActions("mail", {
            $_RemoveMixin_remove: REMOVE_MESSAGES,
            $_RemoveMixin_move: MOVE_MESSAGES_TO_TRASH,
            $_RemoveMixin_removeConversations: REMOVE_CONVERSATIONS,
            $_RemoveMixin_moveConversationsToTrash: MOVE_CONVERSATIONS_TO_TRASH
        }),
        MOVE_CONVERSATIONS_TO_TRASH: navigateConversations(async function (conversations) {
            const trash = this.$_RemoveMixin_trash;

            if (conversations.some(conversation => conversation.folderRef.key !== trash.key)) {
                this.$_RemoveMixin_moveConversationsToTrash({
                    conversations,
                    folder: trash
                });
                return true;
            } else {
                return await this.REMOVE_CONVERSATIONS(conversations);
            }
        }),
        REMOVE_CONVERSATIONS: navigateConversations(async function (conversations) {
            const onlyConversations = conversationsOnly(conversations);
            const textKey = onlyConversations
                ? "mail.actions.purge.conversations.modal.content"
                : "mail.actions.purge.modal.content";
            const titleKey = onlyConversations
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
                this.$_RemoveMixin_removeConversations({ conversations });
            }
            return confirm;
        }),
        MOVE_MESSAGES_TO_TRASH: navigate(async function (conversation, messages) {
            const trash = this.$_RemoveMixin_trash;

            if (messages.some(message => message.folderRef.key !== trash.key)) {
                this.$_RemoveMixin_move({ conversation, messages, folder: trash });
                return true;
            } else {
                return await this.REMOVE_MESSAGES({ conversation, messages });
            }
        }),
        REMOVE_MESSAGES: navigate(async function ({ conversation, messages }) {
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
                this.$_RemoveMixin_remove({ conversation, messages });
            }
            return confirm;
        }),
        async REMOVE_DRAFT(conversation, draft) {
            const formattedDate = this.formatDraftSaveDate(draft);
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
                this.$_RemoveMixin_remove({ conversation, messages: [draft] });
            }
        },
        moveToTrash() {
            return this.MOVE_CONVERSATIONS_TO_TRASH(this.selected);
        },
        remove() {
            return this.REMOVE_CONVERSATIONS(this.selected);
        }
    }
};

function navigate(action) {
    return async function (conversation, messages) {
        messages = Array.isArray(messages) ? [...messages] : [messages];
        const confirm = await action.call(this, conversation, messages);
        if (confirm && messages.length === 1 && this.$store.getters["mail/" + IS_CURRENT_CONVERSATION](messages[0])) {
            const next = this.$store.getters["mail/" + NEXT_CONVERSATION];
            this.$router.navigate({ name: "v:mail:conversation", params: { conversation: next } });
        }
    };
}

function navigateConversations(action) {
    return async function (conversations) {
        const confirm = await action.call(this, conversations);
        if (
            confirm &&
            conversations.length === 1 &&
            this.$store.getters["mail/" + IS_CURRENT_CONVERSATION](conversations[0])
        ) {
            const next = this.$store.getters["mail/" + NEXT_CONVERSATION];
            this.$router.navigate({ name: "v:mail:conversation", params: { conversation: next } });
        }
    };
}
