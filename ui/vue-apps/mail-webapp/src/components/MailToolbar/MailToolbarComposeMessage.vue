<template>
    <div>
        <bm-button
            v-bm-tooltip.bottom
            variant="simple-dark"
            :aria-label="$tc('mail.actions.send.aria')"
            :title="$tc('mail.actions.send.aria')"
            :disabled="isSending || !hasRecipient"
            @click="doSend()"
        >
            <bm-icon icon="send" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.send") }}</span>
        </bm-button>
        <bm-button
            v-bm-tooltip.bottom
            variant="simple-dark"
            :aria-label="$tc('mail.actions.attach.aria')"
            :title="$tc('mail.actions.attach.aria')"
            :disabled="isSending"
            @click="openFilePicker()"
        >
            <bm-icon icon="paper-clip" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.attach") }}</span>
        </bm-button>
        <input
            ref="attachInputRef"
            type="file"
            multiple
            hidden
            @change="
                ADD_ATTACHMENTS({
                    messageKey,
                    files: $event.target.files,
                    userPrefTextOnly,
                    myDraftsFolderKey: MY_DRAFTS.key,
                    editorContent: messageCompose.editorContent
                })
            "
        />
        <bm-button
            v-bm-tooltip.bottom
            variant="simple-dark"
            :aria-label="$tc('mail.actions.save.aria')"
            :title="$tc('mail.actions.save.aria')"
            :disabled="isSaving || isSending"
            @click="
                SAVE_MESSAGE({
                    userPrefTextOnly,
                    draftKey: messageKey,
                    myDraftsFolderKey: MY_DRAFTS.key,
                    editorContent: messageCompose.editorContent
                })
            "
        >
            <bm-icon icon="save" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.save") }}</span>
        </bm-button>
        <bm-button
            v-bm-tooltip.bottom
            variant="simple-dark"
            :aria-label="$tc('mail.actions.remove.compose.aria')"
            :title="$tc('mail.actions.remove.compose.aria')"
            :disabled="isSaving || isSending"
            @click="doDelete()"
        >
            <bm-icon icon="trash" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.remove") }}</span>
        </bm-button>
    </div>
</template>

<script>
import { mapActions, mapState } from "vuex";

import { BmButton, BmIcon, BmTooltip } from "@bluemind/styleguide";

import actionTypes from "../../store/actionTypes";
import MessageStatus from "../../store/messages/MessageStatus";
import { mapGetters } from "vuex";

export default {
    name: "MailToolbarComposeMessage",
    components: {
        BmButton,
        BmIcon
    },
    directives: { BmTooltip },
    data() {
        return {
            userPrefTextOnly: false // TODO: initialize this with user setting
        };
    },
    computed: {
        ...mapState("mail-webapp/currentMessage", { messageKey: "key" }),
        ...mapState("mail", ["messageCompose", "messages"]),
        ...mapGetters("mail", ["MY_DRAFTS", "MY_OUTBOX", "MY_SENT", "MY_MAILBOX_KEY"]),
        message() {
            return this.messages[this.messageKey];
        },
        hasRecipient() {
            return this.message.to.length > 0 || this.message.cc.length > 0 || this.message.bcc.length > 0;
        },
        isSending() {
            return this.message.status === MessageStatus.SENDING;
        },
        isSaving() {
            return this.message.status === MessageStatus.SAVING;
        },
        errorOccuredOnSave() {
            return this.message.status === MessageStatus.SAVE_ERROR;
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["purge"]),
        ...mapActions("mail", [actionTypes.ADD_ATTACHMENTS, actionTypes.SAVE_MESSAGE, actionTypes.SEND_MESSAGE]),
        async doDelete() {
            const confirm = await this.$bvModal.msgBoxConfirm(this.$t("mail.draft.delete.confirm.content"), {
                title: this.$t("mail.draft.delete.confirm.title"),
                okTitle: this.$t("common.delete"),
                cancelVariant: "outline-secondary",
                cancelTitle: this.$t("common.cancel"),
                centered: true,
                hideHeaderClose: false
            });
            if (confirm) {
                this.purge(this.messageKey);
                this.$router.navigate("v:mail:home");
            }
        },
        doSend() {
            this.SEND_MESSAGE({
                userPrefTextOnly: this.userPrefTextOnly,
                draftKey: this.messageKey,
                myMailboxKey: this.MY_MAILBOX_KEY,
                outboxId: this.MY_OUTBOX.id,
                myDraftsFolder: this.MY_DRAFTS,
                sentFolder: this.MY_SENT,
                editorContent: this.messageCompose.editorContent
            });
            this.$router.navigate("v:mail:home");
        },
        openFilePicker() {
            this.$refs.attachInputRef.click();
        }
    }
};
</script>
