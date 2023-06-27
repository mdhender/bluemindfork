import cloneDeep from "lodash.clonedeep";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { ERROR, REMOVE } from "@bluemind/alert.store";
import { inject } from "@bluemind/inject";
import { draftUtils, fileUtils, messageUtils } from "@bluemind/mail";
import { ContactValidator } from "@bluemind/contact";

import {
    DEBOUNCED_SAVE_MESSAGE,
    REMOVE_CONVERSATION_MESSAGES,
    SAVE_MESSAGE,
    SAVE_AS_DRAFT,
    SAVE_AS_TEMPLATE,
    SEND_MESSAGE
} from "~/actions";
import {
    CONVERSATIONS_ACTIVATED,
    CURRENT_CONVERSATION_METADATA,
    MY_DRAFTS,
    MY_OUTBOX,
    MY_SENT,
    MY_TEMPLATES,
    MY_MAILBOX_KEY
} from "~/getters";
import {
    ADD_MESSAGES,
    RESET_PARTS_DATA,
    SET_ACTIVE_MESSAGE,
    SET_MESSAGE_COMPOSING,
    SET_MESSAGE_SUBJECT
} from "~/mutations";

const { isNewMessage, createFromDraft } = draftUtils;
const { FileStatus } = fileUtils;
const { MessageStatus } = messageUtils;
import { MAX_RECIPIENTS } from "../utils";

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
            userPrefTextOnly: false, // FIXME: https://forge.bluemind.net/jira/browse/FEATWEBML-88
            userSession: inject("UserSession")
        };
    },
    computed: {
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapGetters("mail", {
            $_ComposerActionsMixin_CONVERSATIONS_ACTIVATED: CONVERSATIONS_ACTIVATED,
            $_ComposerActionsMixin_CURRENT_CONVERSATION_METADATA: CURRENT_CONVERSATION_METADATA,
            $_ComposerActionsMixin_MY_DRAFTS: MY_DRAFTS,
            $_ComposerActionsMixin_MY_OUTBOX: MY_OUTBOX,
            $_ComposerActionsMixin_MY_SENT: MY_SENT,
            $_ComposerActionsMixin_MY_MAILBOX_KEY: MY_MAILBOX_KEY
        }),
        ...mapState("mail", {
            $_ComposerActionsMixin_messageCompose: "messageCompose",
            $_ComposerActionsMixin_currentConversation: ({ conversations }) => conversations.currentConversation
        }),
        isDraft() {
            return this.message.folderRef.key === this.$_ComposerActionsMixin_MY_DRAFTS.key;
        },
        isTemplate() {
            return this.message.folderRef.key === this.$store.getters[`mail/${MY_TEMPLATES}`].key;
        },
        anyAttachmentInError() {
            return this.message.attachments.some(a => a.status === FileStatus.ERROR);
        },
        maxRecipientsExceeded() {
            return this.message.to.length + this.message.cc.length + this.message.bcc.length > MAX_RECIPIENTS;
        },
        hasRecipient() {
            return this.message.to.length > 0 || this.message.cc.length > 0 || this.message.bcc.length > 0;
        },
        isSending() {
            return this.message.status === MessageStatus.SENDING;
        },
        isInvalid() {
            return this.message.status === MessageStatus.INVALID;
        },
        errorOccuredOnSave() {
            return this.message.status === MessageStatus.SAVE_ERROR;
        },
        anyRecipientInError() {
            return this.message.to
                .concat(this.message.cc)
                .concat(this.message.bcc)
                .some(contact => !ContactValidator.validateContact(contact));
        },
        isSendingDisabled() {
            return (
                this.errorOccuredOnSave ||
                this.isInvalid ||
                this.isSending ||
                !this.hasRecipient ||
                this.anyRecipientInError ||
                this.anyAttachmentInError ||
                this.maxRecipientsExceeded
            );
        }
    },

    methods: {
        ...mapActions("alert", { ERROR, REMOVE }),
        ...mapActions("mail", {
            $_ComposerActionsMixin_SAVE_MESSAGE: SAVE_MESSAGE,
            $_ComposerActionsMixin_SEND_MESSAGE: SEND_MESSAGE,
            $_ComposerActionsMixin_DEBOUNCED_SAVE: DEBOUNCED_SAVE_MESSAGE
        }),
        ...mapMutations("mail", { $_ComposerActionsMixin_ADD_MESSAGES: ADD_MESSAGES }),
        async debouncedSave() {
            await this.$_ComposerActionsMixin_DEBOUNCED_SAVE({
                draft: this.message,
                messageCompose: cloneDeep(this.$_ComposerActionsMixin_messageCompose),
                files: this.message.attachments.map(({ fileKey }) => this.$store.state.mail.files[fileKey])
            });
        },
        async saveAsap() {
            await this.$_ComposerActionsMixin_SAVE_MESSAGE({
                draft: this.message,
                messageCompose: cloneDeep(this.$_ComposerActionsMixin_messageCompose),
                files: this.message.attachments.map(({ fileKey }) => this.$store.state.mail.files[fileKey])
            });
        },
        async saveMessageAs(saveAction, folder) {
            const message = createFromDraft(this.message, folder);
            this.$store.commit(`mail/${ADD_MESSAGES}`, { messages: [message] });

            await this.$store.dispatch(`mail/${saveAction}`, {
                message,
                messageCompose: this.$_ComposerActionsMixin_messageCompose,
                files: this.message.attachments.map(({ fileKey }) => this.$store.state.mail.files[fileKey])
            });
            this.$router.navigate({ name: "v:mail:message", params: { message } });
            this.$store.commit(`mail/${SET_ACTIVE_MESSAGE}`, message);
        },
        async saveAsTemplate() {
            if (this.message.folderRef.key !== this.$store.getters[`mail/${MY_TEMPLATES}`].key) {
                this.saveMessageAs(SAVE_AS_TEMPLATE, this.$store.getters[`mail/${MY_TEMPLATES}`]);
            } else {
                this.saveAsap();
            }
        },
        async saveAsDraft() {
            if (this.message.folderRef.key !== this.$store.getters[`mail/${MY_DRAFTS}`].key) {
                this.saveMessageAs(SAVE_AS_DRAFT, this.$store.getters[`mail/${MY_DRAFTS}`]);
            } else {
                return this.saveAsap();
            }
        },
        updateSubject(subject) {
            this.$store.commit("mail/" + SET_MESSAGE_SUBJECT, { messageKey: this.message.key, subject });
            this.debouncedSave();
        },
        async deleteDraft() {
            const isNew = isNewMessage(this.message);
            let confirmed;
            if (!isNew) {
                const kind = this.isDraft ? "draft" : "template";
                confirmed = await this.$bvModal.msgBoxConfirm(this.$t(`mail.compose.confirm_delete.${kind}.content`), {
                    title: this.$t(`mail.compose.confirm_delete.${kind}.title`),
                    okTitle: this.$t("common.delete"),
                    cancelTitle: this.$t("common.cancel"),
                    okVariant: "fill-accent",
                    cancelVariant: "text",
                    centered: true,
                    hideHeaderClose: false,
                    autoFocusButton: "ok"
                });
            }
            if (isNew || confirmed) {
                await this.$store.dispatch(`mail/${REMOVE_CONVERSATION_MESSAGES}`, {
                    conversation: this.$_ComposerActionsMixin_CURRENT_CONVERSATION_METADATA,
                    messages: [this.message]
                });
                this.removeAttachmentAndInlineTmpParts();
                if (!this.$_ComposerActionsMixin_CONVERSATIONS_ACTIVATED) {
                    this.$router.navigate("v:mail:home");
                } else {
                    this.$router.navigate({
                        name: "v:mail:conversation",
                        params: { conversation: this.$_ComposerActionsMixin_CURRENT_CONVERSATION_METADATA }
                    });
                }
            }
        },
        send() {
            this.$_ComposerActionsMixin_SEND_MESSAGE({
                draft: this.message,
                myMailboxKey: this.$_ComposerActionsMixin_MY_MAILBOX_KEY,
                outbox: this.$_ComposerActionsMixin_MY_OUTBOX,
                myDraftsFolder: this.$_ComposerActionsMixin_MY_DRAFTS,
                messageCompose: cloneDeep(this.$_ComposerActionsMixin_messageCompose),
                files: this.message.attachments.map(({ fileKey }) => this.$store.state.mail.files[fileKey]),
                subject: this.message.subject
            });
            if (
                !this.$_ComposerActionsMixin_CONVERSATIONS_ACTIVATED ||
                !this.$_ComposerActionsMixin_currentConversation ||
                this.$_ComposerActionsMixin_CURRENT_CONVERSATION_METADATA.messages.length < 2
            ) {
                this.$router.navigate("v:mail:home");
            }
        },
        removeAttachmentAndInlineTmpParts() {
            const addresses = this.message.attachments
                .concat(this.$_ComposerActionsMixin_messageCompose.inlineImagesSaved)
                .map(part => part.address);
            if (addresses?.length) {
                const service = inject("MailboxItemsPersistence", this.message.folderRef.uid);
                addresses.forEach(address => service.removePart(address));
            }
        },
        async endEdition() {
            await this.saveAsap();
            if (
                this.$_ComposerActionsMixin_currentConversation &&
                this.$_ComposerActionsMixin_currentConversation === this.message.conversationRef?.key
            ) {
                this.$store.commit(`mail/${RESET_PARTS_DATA}`);
            } else {
                this.$router.navigate("v:mail:home");
            }
            this.$store.commit(`mail/${SET_MESSAGE_COMPOSING}`, { messageKey: this.message.key, composing: false });
        }
    }
};
