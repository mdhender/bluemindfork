<template>
    <bm-button-group class="mail-toolbar-compose-message">
        <mail-toolbar-responsive-button
            v-if="isDraft"
            :aria-label="$t('mail.actions.send.aria')"
            :title="$t('mail.actions.send.aria')"
            :disabled="errorOccuredOnSave || isSending || !hasRecipient || anyAttachmentInError"
            icon="send"
            :label="$tc('mail.actions.send')"
            @click="send()"
        />
        <mail-toolbar-responsive-button
            v-else
            :aria-label="$t('mail.actions.end_template_edition.aria')"
            :title="$t('mail.actions.end_template_edition.aria')"
            icon="arrow-back"
            :label="$tc('mail.actions.end_template_edition.label')"
            @click="endEdition"
        />
        <mail-toolbar-responsive-button
            :aria-label="$t('mail.actions.attach.aria')"
            :title="$t('mail.actions.attach.aria')"
            :disabled="isSending"
            icon="paper-clip"
            :label="$tc('mail.actions.attach')"
            @click="openFilePicker()"
        />
        <input
            ref="attachInputRef"
            type="file"
            multiple
            hidden
            @change="$execute('add-attachments', { files: $event.target.files, message, maxSize })"
            @click="closeFilePicker()"
        />
        <mail-toolbar-responsive-dropdown
            :aria-label="saveActionTitle"
            :title="saveActionTitle"
            :disabled="isSaving || isSending || anyAttachmentInError"
            :icon="isDraft ? 'save' : 'plus-document'"
            :label="$t('common.save')"
            split
            right
            @click="saveAsap"
        >
            <bm-dropdown-item icon="save" @click="saveAsDraft">{{ $t("mail.actions.save_draft") }}</bm-dropdown-item>
            <bm-dropdown-item icon="plus-document" @click="saveAsTemplate">{{
                $t("mail.actions.save_template")
            }}</bm-dropdown-item>
        </mail-toolbar-responsive-dropdown>
        <mail-toolbar-responsive-button
            :aria-label="$tc('mail.actions.remove.compose.aria')"
            :title="$tc('mail.actions.remove.compose.aria')"
            :disabled="isSaving || isSending"
            icon="trash"
            :label="$tc('mail.actions.remove')"
            @click="deleteDraft"
        />
        <mail-toolbar-responsive-dropdown
            ref="other-dropdown"
            :aria-label="$tc('mail.toolbar.more.aria')"
            :title="$tc('mail.toolbar.more.aria')"
            icon="3dots"
            :label="$t('mail.toolbar.more')"
            no-caret
            class="other-viewer-actions"
        >
            <bm-dropdown-item :disabled="isSenderShown" @click="showSender">
                {{ $tc("mail.actions.show_sender", 1) }}
            </bm-dropdown-item>
        </mail-toolbar-responsive-dropdown>
    </bm-button-group>
</template>

<script>
import { mapGetters, mapMutations } from "vuex";

import { BmButtonGroup, BmDropdownItem } from "@bluemind/styleguide";
import { messageUtils } from "@bluemind/mail";
import MailToolbarResponsiveButton from "./MailToolbarResponsiveButton";
import MailToolbarResponsiveDropdown from "./MailToolbarResponsiveDropdown";
import { ComposerActionsMixin } from "~/mixins";
import { AddAttachmentsCommand } from "~/commands";
import { IS_SENDER_SHOWN, MY_DRAFTS } from "~/getters";
import { SHOW_SENDER } from "~/mutations";

const { MessageStatus } = messageUtils;

export default {
    name: "MailToolbarComposeMessage",
    components: {
        MailToolbarResponsiveButton,
        MailToolbarResponsiveDropdown,
        BmButtonGroup,
        BmDropdownItem
    },
    mixins: [AddAttachmentsCommand, ComposerActionsMixin],
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", { IS_SENDER_SHOWN, MY_DRAFTS }),
        userSettings() {
            return this.$store.state.settings;
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
        },
        saveActionTitle() {
            if (this.isDraft) {
                return this.$t("mail.actions.save_draft");
            } else {
                return this.$t("mail.actions.save_template");
            }
        },
        isSenderShown() {
            return this.IS_SENDER_SHOWN(this.userSettings);
        }
    },
    methods: {
        ...mapMutations("mail", { SHOW_SENDER }),
        openFilePicker() {
            this.$refs.attachInputRef.click();
        },
        closeFilePicker() {
            this.$refs.attachInputRef.value = "";
        },
        showSender() {
            this.SHOW_SENDER(true);
            this.$refs["other-dropdown"].hide(false);
        }
    }
};
</script>
