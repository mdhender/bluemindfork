<template>
    <bm-button-group>
        <bm-button
            v-if="isDraft"
            variant="inline-light"
            class="btn-lg-simple-dark"
            :aria-label="$t('mail.actions.send.aria')"
            :title="$t('mail.actions.send.aria')"
            :disabled="errorOccuredOnSave || isSending || !hasRecipient"
            @click="send()"
        >
            <bm-icon icon="send" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.actions.send") }}</span>
        </bm-button>
        <bm-button
            v-else
            variant="inline-light"
            class="d-none d-lg-block btn-lg-simple-dark"
            :title="$t('mail.actions.end_template_edition.aria')"
            :aria-label="$t('mail.actions.end_template_edition.aria')"
            @click="endEdition"
        >
            <bm-icon icon="arrow-back" size="2x" />
            <span>{{ $tc("mail.actions.end_template_edition.label") }}</span>
        </bm-button>
        <bm-button
            variant="inline-light"
            class="btn-lg-simple-dark"
            :aria-label="$t('mail.actions.attach.aria')"
            :title="$t('mail.actions.attach.aria')"
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
