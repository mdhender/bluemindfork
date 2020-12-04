<template>
    <div>
        <bm-button
            variant="inline-light"
            class="btn-lg-simple-dark"
            :aria-label="$tc('mail.actions.send.aria')"
            :title="$tc('mail.actions.send.aria')"
            :disabled="isSending || !hasRecipient"
            @click="send()"
        >
            <bm-icon icon="send" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.send") }}</span>
        </bm-button>
        <bm-button
            variant="inline-light"
            class="btn-lg-simple-dark"
            :aria-label="$tc('mail.actions.attach.aria')"
            :title="$tc('mail.actions.attach.aria')"
            :disabled="isSending"
            @click="openFilePicker()"
        >
            <bm-icon icon="paper-clip" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.attach") }}</span>
        </bm-button>
        <input ref="attachInputRef" type="file" multiple hidden @change="addAttachments($event.target.files)" />
        <bm-button
            variant="inline-light"
            class="btn-lg-simple-dark"
            :aria-label="$tc('mail.actions.save.aria')"
            :title="$tc('mail.actions.save.aria')"
            :disabled="isSaving || isSending"
            @click="saveAsap"
        >
            <bm-icon icon="save" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.save") }}</span>
        </bm-button>
        <bm-button
            variant="inline-light"
            class="btn-lg-simple-dark"
            :aria-label="$tc('mail.actions.remove.compose.aria')"
            :title="$tc('mail.actions.remove.compose.aria')"
            :disabled="isSaving || isSending"
            @click="deleteDraft"
        >
            <bm-icon icon="trash" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.remove") }}</span>
        </bm-button>
    </div>
</template>

<script>
import { mapState } from "vuex";

import { BmButton, BmIcon } from "@bluemind/styleguide";

import { ComposerActionsMixin } from "~mixins";
import { MessageStatus } from "../../model/message";

export default {
    name: "MailToolbarComposeMessage",
    components: {
        BmButton,
        BmIcon
    },
    mixins: [ComposerActionsMixin],
    props: {
        messageKey: {
            type: String,
            required: true
        }
    },
    computed: {
        ...mapState("mail", ["messages"]),
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
        openFilePicker() {
            this.$refs.attachInputRef.click();
        }
    }
};
</script>
