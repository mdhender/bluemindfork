import cloneDeep from "lodash.clonedeep";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { inject } from "@bluemind/inject";
import { draftUtils, attachmentUtils } from "@bluemind/mail";

import {
    DEBOUNCED_SAVE_MESSAGE,
    REMOVE_ATTACHMENT,
    REMOVE_CONVERSATION_MESSAGES,
    SAVE_MESSAGE,
    SAVE_AS_DRAFT,
    SAVE_AS_TEMPLATE,
    SEND_MESSAGE
} from "~/actions";
import {
    ACTIVE_MESSAGE,
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
    REMOVE_MESSAGES,
    RESET_PARTS_DATA,
    SET_ACTIVE_MESSAGE,
    SET_MESSAGE_COMPOSING,
    SET_MESSAGE_SUBJECT
} from "~/mutations";

const { isNewMessage, createFromDraft } = draftUtils;
const { AttachmentStatus } = attachmentUtils;

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
            return this.message.attachments.some(a => a.status === AttachmentStatus.ERROR);
        }
    },

    methods: {
        ...mapActions("mail", {
            $_ComposerActionsMixin_SAVE_MESSAGE: SAVE_MESSAGE,
            $_ComposerActionsMixin_SEND_MESSAGE: SEND_MESSAGE,
            $_ComposerActionsMixin_DEBOUNCED_SAVE: DEBOUNCED_SAVE_MESSAGE,
            $_ComposerActionsMixin_REMOVE_ATTACHMENT: REMOVE_ATTACHMENT
        }),
        ...mapMutations("mail", { $_ComposerActionsMixin_ADD_MESSAGES: ADD_MESSAGES }),
        async debouncedSave() {
            const wasMessageOnlyLocal = isNewMessage(this.message);
            await this.$_ComposerActionsMixin_DEBOUNCED_SAVE({
                draft: this.message,
                messageCompose: cloneDeep(this.$_ComposerActionsMixin_messageCompose)
            });
            this.updateRoute(wasMessageOnlyLocal);
        },
        async saveAsap() {
            const wasMessageOnlyLocal = isNewMessage(this.message);
            await this.$_ComposerActionsMixin_SAVE_MESSAGE({
                draft: this.message,
                messageCompose: cloneDeep(this.$_ComposerActionsMixin_messageCompose)
            });
            this.updateRoute(wasMessageOnlyLocal);
        },
        async saveMessageAs(saveAction, folder) {
            const message = createFromDraft(this.message, folder);
            this.$store.commit(`mail/${ADD_MESSAGES}`, { messages: [message] });

            await this.$store.dispatch(`mail/${saveAction}`, {
                message,
                messageCompose: this.$_ComposerActionsMixin_messageCompose
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
        updateRoute(wasMessageOnlyLocal) {
            const displayedInConversationMode =
                this.$_ComposerActionsMixin_CONVERSATIONS_ACTIVATED &&
                this.$_ComposerActionsMixin_currentConversation &&
                this.$_ComposerActionsMixin_CURRENT_CONVERSATION_METADATA.messages.length > 1;

            if (
                wasMessageOnlyLocal &&
                this.$store.getters["mail/" + ACTIVE_MESSAGE]?.key === this.message.key &&
                !displayedInConversationMode
            ) {
                this.$router.navigate({ name: "v:mail:message", params: { message: this.message } });
            }
        },
        updateSubject(subject) {
            this.$store.commit("mail/" + SET_MESSAGE_SUBJECT, { messageKey: this.message.key, subject });
            this.debouncedSave();
        },
        async deleteDraft() {
            if (isNewMessage(this.message)) {
                this.$store.commit(`mail/${REMOVE_MESSAGES}`, { messages: [this.message] });
                if (!this.$_ComposerActionsMixin_currentConversation) {
                    this.$router.navigate("v:mail:home");
                } else {
                    this.$router.navigate({
                        name: "v:mail:conversation",
                        params: { conversation: this.$_ComposerActionsMixin_CURRENT_CONVERSATION_METADATA }
                    });
                }
            } else {
                const kind = this.isDraft ? "draft" : "template";
                const confirm = await this.$bvModal.msgBoxConfirm(
                    this.$t(`mail.compose.confirm_delete.${kind}.content`),
                    {
                        title: this.$t(`mail.compose.confirm_delete.${kind}.title`),
                        okTitle: this.$t("common.delete"),
                        cancelTitle: this.$t("common.cancel"),
                        okVariant: "secondary",
                        cancelVariant: "simple-neutral",
                        centered: true,
                        hideHeaderClose: false,
                        autoFocusButton: "ok"
                    }
                );
                if (confirm) {
                    const conversation = this.$_ComposerActionsMixin_CURRENT_CONVERSATION_METADATA;
                    await this.$store.dispatch(`mail/${REMOVE_CONVERSATION_MESSAGES}`, {
                        conversation,
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
            }
        },
        send() {
            this.$_ComposerActionsMixin_SEND_MESSAGE({
                draftKey: this.message.key,
                myMailboxKey: this.$_ComposerActionsMixin_MY_MAILBOX_KEY,
                outbox: this.$_ComposerActionsMixin_MY_OUTBOX,
                myDraftsFolder: this.$_ComposerActionsMixin_MY_DRAFTS,
                messageCompose: cloneDeep(this.$_ComposerActionsMixin_messageCompose)
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
            const service = inject("MailboxItemsPersistence", this.message.folderRef.uid);
            const addresses = this.message.attachments
                .concat(this.$_ComposerActionsMixin_messageCompose.inlineImagesSaved)
                .map(part => part.address);
            addresses.forEach(address => service.removePart(address));
        },
        async endEdition() {
            await this.saveAsap();
            if (this.$_ComposerActionsMixin_currentConversation === this.message.conversationRef.key) {
                this.$store.commit(`mail/${RESET_PARTS_DATA}`);
            } else {
                this.$router.navigate("v:mail:home");
            }
            this.$store.commit(`mail/${SET_MESSAGE_COMPOSING}`, { messageKey: this.message.key, composing: false });
        }
    }
};
