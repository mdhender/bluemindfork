<template>
    <bm-toolbar class="mail-toolbar-compose-message" :class="{ compact }">
        <mail-toolbar-responsive-button
            v-if="isDraft"
            :aria-label="$t('mail.actions.send.aria')"
            :title="$t('mail.actions.send.aria')"
            :disabled="errorOccuredOnSave || isSending || !hasRecipient || anyAttachmentInError || isInvalid"
            icon="send"
            :label="$tc('mail.actions.send')"
            :compact="compact"
            @click="send()"
        />
        <mail-toolbar-responsive-button
            v-else
            :aria-label="$t('mail.actions.end_template_edition.aria')"
            :title="$t('mail.actions.end_template_edition.aria')"
            icon="arrow-left"
            :label="$tc('mail.actions.end_template_edition.label')"
            :compact="compact"
            @click="endEdition"
        />
        <mail-toolbar-responsive-button
            :aria-label="$t('mail.actions.attach.aria')"
            :title="$t('mail.actions.attach.aria')"
            :disabled="isSending"
            icon="paperclip"
            :label="$tc('mail.actions.attach')"
            :compact="compact"
            @click="openFilePicker()"
        />
        <mail-toolbar-responsive-dropdown
            :aria-label="saveActionTitle"
            :title="saveActionTitle"
            :disabled="isSaving || isSending || anyAttachmentInError || isInvalid"
            :icon="isDraft ? 'save' : 'document-plus'"
            :label="$t('common.save')"
            split
            right
            :compact="compact"
            @click="saveAsap"
        >
            <bm-dropdown-item icon="save" @click="saveAsDraft">{{ $t("mail.actions.save_draft") }}</bm-dropdown-item>
            <bm-dropdown-item icon="document-plus" @click="saveAsTemplate">{{
                $t("mail.actions.save_template")
            }}</bm-dropdown-item>
        </mail-toolbar-responsive-dropdown>
        <mail-toolbar-responsive-button
            :aria-label="$tc('mail.actions.remove.compose.aria')"
            :title="$tc('mail.actions.remove.compose.aria')"
            :disabled="isSaving || isSending"
            icon="trash"
            :text="$tc('mail.actions.remove')"
            :label="$tc('mail.actions.remove')"
            :compact="compact"
            @click="deleteDraft"
        />
        <template #menu-button>
            <mail-toolbar-menu-button :compact="compact" />
        </template>
        <template #menu>
            <bm-dropdown-item :disabled="isSenderShown" @click="showSender">
                {{ $tc("mail.actions.show_sender", 1) }}
            </bm-dropdown-item>
            <bm-dropdown-item icon="code" :disabled="!canShowOrDownloadEml" @click.stop="showSource(message)">
                {{ $t("mail.actions.show_source") }}
            </bm-dropdown-item>
            <bm-dropdown-item
                icon="box-arrow-down"
                :disabled="!canShowOrDownloadEml"
                @click.stop="downloadEml(message)"
            >
                {{ $t("mail.actions.download_eml") }}
            </bm-dropdown-item></template
        >
        <input
            ref="attachInputRef"
            type="file"
            multiple
            hidden
            @change="$execute('add-attachments', { files: $event.target.files, message })"
            @click="closeFilePicker()"
        />
    </bm-toolbar>
</template>

<script>
import { mapGetters, mapMutations } from "vuex";

import { BmToolbar, BmDropdownItem } from "@bluemind/ui-components";
import { messageUtils } from "@bluemind/mail";
import MailToolbarResponsiveButton from "./MailToolbarResponsiveButton";
import MailToolbarResponsiveDropdown from "./MailToolbarResponsiveDropdown";
import { ComposerActionsMixin, EmlMixin } from "~/mixins";
import { AddAttachmentsCommand } from "~/commands";
import { IS_SENDER_SHOWN, MY_DRAFTS } from "~/getters";
import { SHOW_SENDER } from "~/mutations";
import MailToolbarMenuButton from "./MailToolbarMenuButton";

const { MessageStatus } = messageUtils;

export default {
    name: "MailToolbarComposeMessage",
    components: {
        BmToolbar,
        MailToolbarResponsiveButton,
        MailToolbarResponsiveDropdown,
        BmDropdownItem,
        MailToolbarMenuButton
    },
    mixins: [AddAttachmentsCommand, ComposerActionsMixin, EmlMixin],
    props: {
        message: {
            type: Object,
            required: true
        },
        compact: {
            type: Boolean,
            default: false
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
        canShowOrDownloadEml() {
            return this.message.status === MessageStatus.IDLE;
        },
        errorOccuredOnSave() {
            return this.message.status === MessageStatus.SAVE_ERROR;
        },
        isInvalid() {
            return this.message.status === MessageStatus.INVALID;
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
