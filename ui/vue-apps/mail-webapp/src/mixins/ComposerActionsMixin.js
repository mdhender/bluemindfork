import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { inject } from "@bluemind/inject";

import {
    ADD_ATTACHMENTS,
    DEBOUNCED_SAVE_MESSAGE,
    REMOVE_ATTACHMENT,
    REMOVE_MESSAGES,
    SAVE_MESSAGE,
    SEND_MESSAGE
} from "~/actions";
import { ACTIVE_MESSAGE, MY_DRAFTS, MY_OUTBOX, MY_SENT, MY_MAILBOX_KEY } from "~/getters";
import { ADD_MESSAGES, REMOVE_NEW_MESSAGE_FROM_CONVERSATION } from "~/mutations";
import { isNewMessage } from "~/model/draft";

/**
 * Provide composition Vuex actions to components
 */
export default {
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            userPrefTextOnly: false // FIXME: https://forge.bluemind.net/jira/browse/FEATWEBML-88
        };
    },
    computed: {
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapGetters("mail", {
            $_ComposerActionsMixin_MY_DRAFTS: MY_DRAFTS,
            $_ComposerActionsMixin_MY_OUTBOX: MY_OUTBOX,
            $_ComposerActionsMixin_MY_SENT: MY_SENT,
            $_ComposerActionsMixin_MY_MAILBOX_KEY: MY_MAILBOX_KEY
        }),
        ...mapState("mail", {
            $_ComposerActionsMixin_messageCompose: "messageCompose",
            $_ComposerActionsMixin_currentConversation: ({ conversations }) => conversations.currentConversation
        }),
        ...mapState("session", { $_ComposerActionsMixin_settings: ({ settings }) => settings.remote }),
        conversationsActivated() {
            return (
                this.$_ComposerActionsMixin_settings.mail_thread === "true" &&
                this.folders[this.activeFolder].allowConversations
            );
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
        ...mapMutations("mail", {
            $_ComposerActionsMixin_ADD_MESSAGES: ADD_MESSAGES,
            $_ComposerActionsMixin_REMOVE_NEW_MESSAGE_FROM_CONVERSATION: REMOVE_NEW_MESSAGE_FROM_CONVERSATION
        }),
        async debouncedSave() {
            const wasMessageOnlyLocal = isNewMessage(this.message);
            await this.$_ComposerActionsMixin_DEBOUNCED_SAVE({
                draft: this.message,
                messageCompose: this.$_ComposerActionsMixin_messageCompose
            });
            this.updateRoute(wasMessageOnlyLocal);
        },
        async saveAsap() {
            const wasMessageOnlyLocal = isNewMessage(this.message);
            await this.$_ComposerActionsMixin_SAVE_MESSAGE({
                draft: this.message,
                messageCompose: this.$_ComposerActionsMixin_messageCompose
            });
            this.updateRoute(wasMessageOnlyLocal);
        },
        updateRoute(wasMessageOnlyLocal) {
            if (wasMessageOnlyLocal && this.$store.getters["mail/" + ACTIVE_MESSAGE]?.key === this.message.key) {
                this.$router.navigate({
                    name: "v:mail:message",
                    params: { message: this.message }
                });
            }
        },
        addAttachments(files) {
            this.$_ComposerActionsMixin_ADD_ATTACHMENTS({
                draft: this.message,
                files,
                messageCompose: this.$_ComposerActionsMixin_messageCompose
            });
        },
        removeAttachment(address) {
            this.$_ComposerActionsMixin_REMOVE_ATTACHMENT({
                messageKey: this.message.key,
                attachmentAddress: address,
                messageCompose: this.$_ComposerActionsMixin_messageCompose
            });
        },
        async deleteDraft() {
            if (isNewMessage(this.message)) {
                if (!this.$_ComposerActionsMixin_currentConversation) {
                    this.$router.navigate("v:mail:home");
                } else {
                    this.$_ComposerActionsMixin_REMOVE_NEW_MESSAGE_FROM_CONVERSATION({
                        conversation: this.$_ComposerActionsMixin_currentConversation,
                        message: this.message
                    });

                    if (this.conversationsActivated) {
                        this.$router.navigate({
                            name: "v:mail:conversation",
                            params: { conversation: this.$_ComposerActionsMixin_currentConversation }
                        });
                    } else {
                        this.$router.navigate({
                            name: "v:mail:message",
                            params: { message: this.$_ComposerActionsMixin_currentConversation }
                        });
                    }
                }
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
                    const conversation = this.$_ComposerActionsMixin_currentConversation;
                    this.$_ComposerActionsMixin_REMOVE_MESSAGES({ conversation, messages: [this.message] });
                    this.removeAttachmentAndInlineTmpParts();

                    if (!this.conversationsActivated) {
                        this.$router.navigate("v:mail:home");
                    }
                }
            }
        },
        send() {
            this.$_ComposerActionsMixin_SEND_MESSAGE({
                draftKey: this.message.key,
                myMailboxKey: this.$_ComposerActionsMixin_MY_MAILBOX_KEY,
                outboxId: this.$_ComposerActionsMixin_MY_OUTBOX.remoteRef.internalId,
                myDraftsFolder: this.$_ComposerActionsMixin_MY_DRAFTS,
                sentFolder: this.$_ComposerActionsMixin_MY_SENT,
                messageCompose: this.$_ComposerActionsMixin_messageCompose
            });
            if (
                !this.conversationsActivated ||
                !this.$_ComposerActionsMixin_currentConversation ||
                this.$_ComposerActionsMixin_currentConversation.messages.length < 2
            ) {
                this.$router.navigate("v:mail:home");
            }
        },
        removeAttachmentAndInlineTmpParts() {
            const service = inject("MailboxItemsPersistence", this.message.folderRef.uid);
            const addresses = this.message.attachments
                .concat(this.$_ComposerActionsMixin_messageCompose.inlineImagesSaved)
                .map(part => part.address);
            addresses.forEach(address => service.removePart(address));
        }
    }
};
