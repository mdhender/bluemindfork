import { mapActions, mapGetters } from "vuex";

import { IS_ACTIVE_MESSAGE, MY_TRASH, NEXT_MESSAGE } from "~getters";
import { REMOVE_MESSAGES, MOVE_MESSAGES_TO_TRASH } from "~actions";

export default {
    computed: {
        ...mapGetters("mail", { $_RemoveMixin_trash: MY_TRASH })
    },
    methods: {
        ...mapActions("mail", { $_RemoveMixin_remove: REMOVE_MESSAGES, $_RemoveMixin_move: MOVE_MESSAGES_TO_TRASH }),
        MOVE_MESSAGES_TO_TRASH: navigate(async function (messages) {
            const trash = this.$_RemoveMixin_trash;

            if (messages.some(message => message.folderRef.key !== trash.key)) {
                this.$_RemoveMixin_move({ messages, folder: trash });
                return true;
            } else {
                return await this.REMOVE_MESSAGES(messages);
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
            return confirm;
        })
    }
};

function navigate(action) {
    return async function (messages) {
        messages = Array.isArray(messages) ? [...messages] : [messages];
        let next = this.$store.getters["mail/" + NEXT_MESSAGE];
        const confirm = await action.call(this, messages);
        if (confirm && messages.length === 1 && this.$store.getters["mail/" + IS_ACTIVE_MESSAGE](messages[0])) {
            this.$router.navigate({ name: "v:mail:message", params: { message: next } });
        }
    };
}
