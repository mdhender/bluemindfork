import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { ADD_ATTACHMENTS, SAVE_MESSAGE, SEND_MESSAGE } from "~actions";
import { MY_DRAFTS, MY_OUTBOX, MY_SENT, MY_MAILBOX_KEY } from "~getters";
import { ADD_MESSAGES } from "~mutations";
import { isInternalIdFaked } from "../model/draft";
import { updateKey } from "../model/message";

/**
 * Contains logic regarding composition : call actions with right parameter, etc.
 */
const SAVE_DRAFT_DEBOUNCE_TIME = 3000;

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
        ...mapActions("mail-webapp", { $_ComposerActionsMixin_purge: "purge" }),
        ...mapActions("mail", {
            $_ComposerActionsMixin_ADD_ATTACHMENTS: ADD_ATTACHMENTS,
            $_ComposerActionsMixin_SAVE_MESSAGE: SAVE_MESSAGE,
            $_ComposerActionsMixin_SEND_MESSAGE: SEND_MESSAGE
        }),
        ...mapMutations("mail", { $_ComposerActionsMixin_ADD_MESSAGES: ADD_MESSAGES }),
        async save(hasDebounce = true) {
            const wasMessageOnlyLocal = isInternalIdFaked(this.$_ComposerActionsMixin_message.remoteRef.internalId);
            await this.$_ComposerActionsMixin_SAVE_MESSAGE({
                userPrefTextOnly: this.userPrefTextOnly,
                draftKey: this.$_ComposerActionsMixin_message.key,
                myDraftsFolderKey: this.$_ComposerActionsMixin_MY_DRAFTS.key,
                messageCompose: this.$_ComposerActionsMixin_messageCompose,
                debounceTime: hasDebounce ? SAVE_DRAFT_DEBOUNCE_TIME : 0
            });
            if (wasMessageOnlyLocal) {
                const message = updateKey(
                    this.$_ComposerActionsMixin_message,
                    this.$_ComposerActionsMixin_message.remoteRef.internalId,
                    this.$_ComposerActionsMixin_MY_DRAFTS
                );
                this.$_ComposerActionsMixin_ADD_MESSAGES([message]);

                this.$router.navigate({ name: "v:mail:message", params: { message: message.key } });
            }
        },
        addAttachments(files) {
            this.$_ComposerActionsMixin_ADD_ATTACHMENTS({
                messageKey: this.$_ComposerActionsMixin_message.key,
                files,
                userPrefTextOnly: this.userPrefTextOnly,
                myDraftsFolderKey: this.$_ComposerActionsMixin_MY_DRAFTS.key,
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
                    this.$_ComposerActionsMixin_purge(this.$_ComposerActionsMixin_message.key);
                    this.$router.navigate("v:mail:home");
                }
            }
        },
        send() {
            this.$_ComposerActionsMixin_SEND_MESSAGE({
                userPrefTextOnly: this.userPrefTextOnly,
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
