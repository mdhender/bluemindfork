import { mapActions, mapGetters, mapState } from "vuex";
import { MY_TRASH } from "~getters";

import { REMOVE_MESSAGES, MOVE_MESSAGES_TO_TRASH } from "~actions";

export default {
    computed: {
        ...mapGetters("mail-webapp", { $_RemoveMixin_next: "nextMessageKey" }),
        ...mapState("mail-webapp/currentMessage", { $_RemoveMixin_current: "key" }),
        ...mapGetters("mail", { $_RemoveMixin_trash: MY_TRASH })
    },
    methods: {
        ...mapActions("mail", { $_RemoveMixin_remove: REMOVE_MESSAGES, $_RemoveMixin_move: MOVE_MESSAGES_TO_TRASH }),
        MOVE_MESSAGES_TO_TRASH: navigate(async function (messages) {
            const trash = this.$_RemoveMixin_trash;

            if (messages.some(message => message.folderRef.key !== trash.key)) {
                this.$_RemoveMixin_move({ messages, folder: trash });
            } else {
                await this.REMOVE_MESSAGES(messages);
            }
        }),
        REMOVE_MESSAGES: navigate(async function (messages) {
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
                this.$_RemoveMixin_remove(messages);
            }
        })
    }
};

function navigate(action) {
    return async function (messages) {
        messages = Array.isArray(messages) ? [...messages] : [messages];
        const next = this.$_RemoveMixin_next;
        await action.call(this, messages);
        if (messages.length === 1 && messages[0].key === this.$_RemoveMixin_current) {
            this.$router.navigate({ name: "v:mail:message", params: { message: next } });
        }
    };
}
