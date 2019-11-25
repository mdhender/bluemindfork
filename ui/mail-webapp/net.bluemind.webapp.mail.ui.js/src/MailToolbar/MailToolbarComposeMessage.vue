<template>
    <div>
        <bm-button
            v-bm-tooltip.bottom.d500
            variant="link"
            :aria-label="$tc('mail.actions.send.aria')"
            :title="$tc('mail.actions.send.aria')"
            :disabled="isSending || isDeleting"
            @click="doSend()"
        >
            <bm-icon icon="send" size="2x" />
            {{ $tc("mail.actions.send") }}
        </bm-button>
        <bm-button
            v-bm-tooltip.bottom.d500
            variant="link"
            :aria-label="$tc('mail.actions.save.aria')"
            :title="$tc('mail.actions.save.aria')"
            :disabled="isSaving || isSending || isDeleting"
            @click="saveDraft()"
        >
            <bm-icon icon="save" size="2x" />
            {{ $tc("mail.actions.save") }}
        </bm-button>
        <bm-button
            v-bm-tooltip.bottom.d500
            variant="link"
            :aria-label="$tc('mail.actions.remove.compose.aria')"
            :title="$tc('mail.actions.remove.compose.aria')"
            :disabled="isSaving || isSending || isDeleting"
            @click="doDelete()"
        >
            <bm-icon icon="trash" size="2x" />
            {{ $tc("mail.actions.remove") }}
        </bm-button> 
        <bm-button
            v-bm-tooltip.bottom.d500
            variant="link"
            :aria-label="$tc('mail.actions.attach.aria')"
            :title="$tc('mail.actions.attach.aria')"
            :disabled="isSending || isDeleting"
        >
            <bm-icon icon="paper-clip" size="2x" />
            {{ $tc("mail.actions.attach") }}
        </bm-button>
    </div>
</template>

<script>
import { BmButton, BmIcon, BmTooltip } from "@bluemind/styleguide";
import { DraftStatus } from "@bluemind/backend.mail.store";
import { mapActions, mapState } from "vuex";
import { RouterMixin } from "@bluemind/router";

export default {
    name: "MailToolbarComposeMessage",
    components: {
        BmButton,
        BmIcon
    },
    directives: { BmTooltip },
    mixins: [ RouterMixin ],
    computed: {
        ...mapState("mail-webapp", ["draft"]),
        isSending() {
            return this.draft.status == DraftStatus.SENDING;
        },
        isSaving() {
            return this.draft.status == DraftStatus.SAVING;
        },
        isDeleting() {
            return this.draft.status == DraftStatus.DELETING;
        },
        errorOccuredOnSave() {
            return this.draft.status == DraftStatus.SAVE_ERROR;
        },
    },
    methods: {
        ...mapActions("mail-webapp", ["deleteDraft", "saveDraft", "send"]),
        doDelete() {
            this.deleteDraft().then(() => this.navigateToParent());
        },
        doSend() {
            this.send().then(() => this.navigateToParent());
        }
    }
};
</script>
