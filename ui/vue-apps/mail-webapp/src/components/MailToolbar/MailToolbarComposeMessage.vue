<template>
    <div>
        <bm-button
            v-bm-tooltip.bottom
            variant="simple-dark"
            :aria-label="$tc('mail.actions.send.aria')"
            :title="$tc('mail.actions.send.aria')"
            :disabled="isSending || isDeleting || !hasRecipient"
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
            :disabled="isSending || isDeleting"
            @click="openFilePicker()"
        >
            <bm-icon icon="paper-clip" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.attach") }}</span>
        </bm-button>
        <input ref="attachInputRef" type="file" multiple hidden @change="addAttachments($event.target.files)" />
        <bm-button
            v-bm-tooltip.bottom
            variant="simple-dark"
            :aria-label="$tc('mail.actions.save.aria')"
            :title="$tc('mail.actions.save.aria')"
            :disabled="isSaving || isSending || isDeleting"
            @click="saveDraft()"
        >
            <bm-icon icon="save" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.save") }}</span>
        </bm-button>
        <bm-button
            v-bm-tooltip.bottom
            variant="simple-dark"
            :aria-label="$tc('mail.actions.remove.compose.aria')"
            :title="$tc('mail.actions.remove.compose.aria')"
            :disabled="isSaving || isSending || isDeleting"
            @click="doDelete()"
        >
            <bm-icon icon="trash" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.remove") }}</span>
        </bm-button>
    </div>
</template>

<script>
import { BmButton, BmIcon, BmTooltip } from "@bluemind/styleguide";
import DraftStatus from "../../store.deprecated/mailbackend/MailboxItemsStore/DraftStatus";
import { mapActions, mapState, mapGetters } from "vuex";

export default {
    name: "MailToolbarComposeMessage",
    components: {
        BmButton,
        BmIcon
    },
    directives: { BmTooltip },
    computed: {
        // FIXME: use messageCompose state
        ...mapState("mail", ["draft"]),
        ...mapGetters("mail-webapp/draft", ["hasRecipient"]),
        isSending() {
            return this.draft.status === DraftStatus.SENDING;
        },
        isSaving() {
            return this.draft.status === DraftStatus.SAVING;
        },
        isDeleting() {
            return this.draft.status === DraftStatus.DELETING;
        },
        errorOccuredOnSave() {
            return this.draft.status === DraftStatus.SAVE_ERROR;
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["deleteDraft", "saveDraft", "send", "addAttachments"]),
        ...mapGetters("mail-webapp/draft", { isMessageEmpty: "isEmpty" }),
        async doDelete() {
            if (this.isMessageEmpty()) {
                await this.$store.dispatch("mail-webapp/deleteDraft");
                this.$router.navigate("v:mail:message");
                return;
            }
            const confirm = await this.$bvModal.msgBoxConfirm(this.$t("mail.draft.delete.confirm.content"), {
                title: this.$t("mail.draft.delete.confirm.title"),
                okTitle: this.$t("common.delete"),
                cancelVariant: "outline-secondary",
                cancelTitle: this.$t("common.cancel"),
                centered: true,
                hideHeaderClose: false
            });
            if (confirm) {
                await this.deleteDraft();
                this.$router.navigate("v:mail:message");
            }
        },
        doSend() {
            // FIXME
            this.send();
            this.$router.navigate("v:mail:home");
        },
        openFilePicker() {
            this.$refs.attachInputRef.click();
        }
    }
};
</script>
