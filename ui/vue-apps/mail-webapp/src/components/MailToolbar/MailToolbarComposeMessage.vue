<template>
    <bm-button-group>
        <bm-button
            v-if="isDraft"
            variant="inline-on-fill-primary"
            class="btn-lg-simple-neutral"
            :aria-label="$t('mail.actions.send.aria')"
            :title="$t('mail.actions.send.aria')"
            :disabled="errorOccuredOnSave || isSending || !hasRecipient || anyAttachmentInError"
            @click="send()"
        >
            <bm-icon icon="send" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.send") }}</span>
        </bm-button>
        <bm-button
            v-else
            variant="inline-on-fill-primary"
            class="d-none d-lg-block btn-lg-simple-neutral"
            :title="$t('mail.actions.end_template_edition.aria')"
            :aria-label="$t('mail.actions.end_template_edition.aria')"
            @click="endEdition"
        >
            <bm-icon icon="arrow-back" size="2x" />
            <span>{{ $tc("mail.actions.end_template_edition.label") }}</span>
        </bm-button>
        <bm-button
            variant="inline-on-fill-primary"
            class="btn-lg-simple-neutral"
            :aria-label="$t('mail.actions.attach.aria')"
            :title="$t('mail.actions.attach.aria')"
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
            @change="$execute('add-attachments', { files: $event.target.files, message, maxSize })"
        />
        <bm-dropdown
            split
            variant="simple-neutral"
            split-class="btn-lg-simple-neutral"
            toggle-class="btn-lg-simple-neutral"
            :disabled="isSaving || isSending || anyAttachmentInError"
            right
            @click="saveAsap"
        >
            <template #button-content>
                <div :title="saveActionTitle">
                    <bm-icon :icon="isDraft ? 'save' : 'plus-document'" size="2x" />
                    <span class="d-none d-lg-block">{{ $t("common.save") }}</span>
                </div>
            </template>
            <bm-dropdown-item icon="save" @click="saveAsDraft">{{ $t("mail.actions.save_draft") }}</bm-dropdown-item>
            <bm-dropdown-item icon="plus-document" @click="saveAsTemplate">{{
                $t("mail.actions.save_template")
            }}</bm-dropdown-item>
        </bm-dropdown>
        <bm-button
            variant="inline-on-fill-primary"
            class="btn-lg-simple-neutral"
            :aria-label="$tc('mail.actions.remove.compose.aria')"
            :title="$tc('mail.actions.remove.compose.aria')"
            :disabled="isSaving || isSending"
            @click="deleteDraft"
        >
            <bm-icon icon="trash" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.remove") }}</span>
        </bm-button>
        <bm-dropdown
            ref="other-dropdown"
            :no-caret="true"
            variant="simple-neutral"
            :aria-label="$tc('mail.toolbar.more.aria')"
            :title="$tc('mail.toolbar.more.aria')"
            class="other-viewer-actions"
        >
            <template slot="button-content">
                <bm-icon icon="3dots" size="2x" />
                <span class="d-none d-lg-block">{{ $t("mail.toolbar.more") }}</span>
            </template>
            <bm-dropdown-item :disabled="isSenderShown" @click="showSender">
                {{ $tc("mail.actions.show_sender", 1) }}
            </bm-dropdown-item>
        </bm-dropdown>
    </bm-button-group>
</template>

<script>
import { mapGetters, mapMutations } from "vuex";

import { BmButton, BmButtonGroup, BmDropdown, BmDropdownItem, BmIcon } from "@bluemind/styleguide";

import { ComposerActionsMixin } from "~/mixins";
import { AddAttachmentsCommand } from "~/commands";
import { MessageStatus } from "~/model/message";
import { IS_SENDER_SHOWN, MY_DRAFTS } from "~/getters";
import { SHOW_SENDER } from "~/mutations";

export default {
    name: "MailToolbarComposeMessage",
    components: {
        BmButton,
        BmButtonGroup,
        BmDropdown,
        BmDropdownItem,
        BmIcon
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
        showSender() {
            this.SHOW_SENDER(true);
            this.$refs["other-dropdown"].hide(false);
        }
    }
};
</script>
