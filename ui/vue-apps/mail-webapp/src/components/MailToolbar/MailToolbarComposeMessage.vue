<template>
    <bm-button-group>
        <bm-button
            variant="inline-light"
            class="btn-lg-simple-dark"
            :aria-label="$tc('mail.actions.send.aria')"
            :title="$tc('mail.actions.send.aria')"
            :disabled="errorOccuredOnSave || isSending || !hasRecipient"
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
        <bm-dropdown
            split
            variant="simple-dark"
            split-class="btn-lg-simple-dark"
            toggle-class="btn-lg-simple-dark"
            :disabled="isSaving || isSending"
            right
            :title="saveActionTitle"
            @click="saveAsap"
        >
            <template v-if="isDraft" #button-content>
                <bm-icon icon="save" size="2x" />
                <span class="d-none d-lg-block">{{ $t("common.save") }}</span>
            </template>
            <template v-else #button-content>
                <bm-icon icon="plus-document" size="2x" />
                <span class="d-none d-lg-block">{{ $t("common.save") }}</span>
            </template>
            <bm-dropdown-item icon="save">{{ $t("mail.actions.save_draft") }}</bm-dropdown-item>
            <bm-dropdown-item icon="plus-document">{{ $t("mail.actions.save_template") }}</bm-dropdown-item>
        </bm-dropdown>
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
    </bm-button-group>
</template>

<script>
import { mapGetters } from "vuex";

import { BmButton, BmButtonGroup, BmDropdown, BmDropdownItem, BmIcon } from "@bluemind/styleguide";

import { ComposerActionsMixin } from "~/mixins";
import { MessageStatus } from "~/model/message";
import { MY_DRAFTS } from "~/getters";

export default {
    name: "MailToolbarComposeMessage",
    components: {
        BmButton,
        BmButtonGroup,
        BmDropdown,
        BmDropdownItem,
        BmIcon
    },
    mixins: [ComposerActionsMixin],
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", { MY_DRAFTS }),
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
        },
        isDraft() {
            return this.message.folderRef.key === this.MY_DRAFTS.key;
        },
        saveActionTitle() {
            if (this.isDraft) {
                return this.$t("mail.actions.save_draft");
            } else {
                return this.$t("mail.actions.save_template");
            }
        }
    },
    methods: {
        openFilePicker() {
            this.$refs.attachInputRef.click();
        }
    }
};
</script>
