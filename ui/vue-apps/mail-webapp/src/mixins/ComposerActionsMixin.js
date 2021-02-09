import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import {
    ADD_ATTACHMENTS,
    DEBOUNCED_SAVE_MESSAGE,
    REMOVE_ATTACHMENT,
    REMOVE_MESSAGES,
    SAVE_MESSAGE,
    SEND_MESSAGE
} from "~actions";
import { MY_DRAFTS, MY_OUTBOX, MY_SENT, MY_MAILBOX_KEY } from "~getters";
import { ADD_MESSAGES } from "~mutations";
import { isInternalIdFaked } from "~model/draft";
import { updateKey } from "~model/message";

/**
 * Provide composition Vuex actions to components
 */
export default {
    props: {
        messageKey: {
            type: String,
            required: true
        }
    },
    data() {
        return {
            userPrefTextOnly: false // FIXME: https://forge.bluemind.net/jira/browse/FEATWEBML-88
        };
    },
    computed: {
        ...mapGetters("mail", {
            $_ComposerActionsMixin_MY_DRAFTS: MY_DRAFTS,
            $_ComposerActionsMixin_MY_OUTBOX: MY_OUTBOX,
            $_ComposerActionsMixin_MY_SENT: MY_SENT,
            $_ComposerActionsMixin_MY_MAILBOX_KEY: MY_MAILBOX_KEY
        }),
        ...mapState("mail", { $_ComposerActionsMixin_messageCompose: "messageCompose" }),
        $_ComposerActionsMixin_message() {
            return this.$store.state.mail.messages[this.messageKey];
        }
    },
    methods: {
        ...mapActions("mail", {
            $_ComposerActionsMixin_ADD_ATTACHMENTS: ADD_ATTACHMENTS,
            $_ComposerActionsMixin_SAVE_MESSAGE: SAVE_MESSAGE,
            $_ComposerActionsMixin_SEND_MESSAGE: SEND_MESSAGE,
            $_ComposerActionsMixin_DEBOUNCED_SAVE: DEBOUNCED_SAVE_MESSAGE,
            $_ComposerActionsMixin_REMOVE_ATTACHMENT: REMOVE_ATTACHMENT,
            $_ComposerActionsMixin_REMOVE_MESSAGES: REMOVE_MESSAGES
        }),
        ...mapMutations("mail", { $_ComposerActionsMixin_ADD_MESSAGES: ADD_MESSAGES }),
        async debouncedSave() {
            const wasMessageOnlyLocal = isInternalIdFaked(this.$_ComposerActionsMixin_message.remoteRef.internalId);
            await this.$_ComposerActionsMixin_DEBOUNCED_SAVE({
                draft: this.$_ComposerActionsMixin_message,
                messageCompose: this.$_ComposerActionsMixin_messageCompose
            });
            this.updateRoute(wasMessageOnlyLocal);
        },
        async saveAsap() {
            const wasMessageOnlyLocal = isInternalIdFaked(this.$_ComposerActionsMixin_message.remoteRef.internalId);
            await this.$_ComposerActionsMixin_SAVE_MESSAGE({
                draft: this.$_ComposerActionsMixin_message,
                messageCompose: this.$_ComposerActionsMixin_messageCompose
            });
            this.updateRoute(wasMessageOnlyLocal);
        },
        updateRoute(wasMessageOnlyLocal) {
            if (wasMessageOnlyLocal) {
                const message = updateKey(
                    this.$_ComposerActionsMixin_message,
                    this.$_ComposerActionsMixin_message.remoteRef.internalId,
                    this.$_ComposerActionsMixin_message.folderRef
                );
                this.$_ComposerActionsMixin_ADD_MESSAGES([message]);
                this.$router.navigate({ name: "v:mail:message", params: { message: message.key } });
            }
        },
        addAttachments(files) {
            this.$_ComposerActionsMixin_ADD_ATTACHMENTS({
                draft: this.$_ComposerActionsMixin_message,
                files,
                messageCompose: this.$_ComposerActionsMixin_messageCompose
            });
        },
        removeAttachment(address) {
            this.$_ComposerActionsMixin_REMOVE_ATTACHMENT({
                messageKey: this.$_ComposerActionsMixin_message.key,
                attachmentAddress: address,
                messageCompose: this.$_ComposerActionsMixin_messageCompose
            });
        },
        async deleteDraft() {
            if (isInternalIdFaked(this.$_ComposerActionsMixin_message.remoteRef.internalId)) {
                this.$router.navigate("v:mail:home");
            } else {
                const confirm = await this.$bvModal.msgBoxConfirm(this.$t("mail.draft.delete.confirm.content"), {
                    title: this.$t("mail.draft.delete.confirm.title"),
                    okTitle: this.$t("common.delete"),
                    cancelVariant: "outline-secondary",
                    cancelTitle: this.$t("common.cancel"),
                    centered: true,
                    hideHeaderClose: false,
                    autoFocusButton: "ok"
                });
                if (confirm) {
                    this.$_ComposerActionsMixin_REMOVE_MESSAGES([this.$_ComposerActionsMixin_message]);
                    this.$router.navigate("v:mail:home");
                }
            }
        },
        send() {
            this.$_ComposerActionsMixin_SEND_MESSAGE({
                draftKey: this.$_ComposerActionsMixin_message.key,
                myMailboxKey: this.$_ComposerActionsMixin_MY_MAILBOX_KEY,
                outboxId: this.$_ComposerActionsMixin_MY_OUTBOX.remoteRef.internalId,
                myDraftsFolder: this.$_ComposerActionsMixin_MY_DRAFTS,
                sentFolder: this.$_ComposerActionsMixin_MY_SENT,
                messageCompose: this.$_ComposerActionsMixin_messageCompose
            });
            this.$router.navigate("v:mail:home");
        }
    }
};
