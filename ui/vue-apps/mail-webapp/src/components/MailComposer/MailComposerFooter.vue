<template>
    <div class="mail-composer-footer p-2 border-top justify-content-between align-items-center">
        <div>
            <bm-button
                type="submit"
                variant="primary"
                :disabled="errorOccuredOnSave || isSending || !hasRecipient"
                @click.prevent="send"
            >
                {{ $t("common.send") }}
            </bm-button>
            <bm-button
                variant="simple-dark"
                class="ml-2"
                :disabled="isSaving || isSending"
                @click.prevent="deleteDraft"
            >
                {{ $t("common.delete") }}
            </bm-button>
        </div>
        <div class="d-flex align-items-center toolbar">
            <span v-if="errorOccuredOnSave" class="pr-2 text-danger" :title="$t('mail.draft.save.error.reason')">
                <!-- trick: modify the viewBox attribute to have a correct vertical alignment -->
                <!-- eslint-disable-next-line vue/attribute-hyphenation -->
                <bm-icon icon="exclamation-circle" class="mr-1" viewBox="0 -1 12 12" />{{ saveMessage }}
            </span>
            <span v-else class="text-muted pr-2">{{ saveMessage }}</span>
            <bm-button
                v-if="!userPrefTextOnly"
                variant="simple-dark"
                :aria-label="textFormatterLabel"
                :title="textFormatterLabel"
                :disabled="isSending"
                @click="$emit('toggle-text-format')"
            >
                <bm-icon icon="text-format" size="lg" />
            </bm-button>
            <input
                ref="attachInputRef"
                tabindex="-1"
                aria-hidden="true"
                type="file"
                multiple
                hidden
                @change="addAttachments($event.target.files)"
            />
            <bm-button
                variant="simple-dark"
                :aria-label="$tc('mail.actions.attach.aria')"
                :title="$tc('mail.actions.attach.aria')"
                :disabled="isSending"
                @click="openFilePicker()"
            >
                <bm-icon icon="paper-clip" size="lg" />
            </bm-button>
            <bm-dropdown v-if="signature" dropup right no-caret variant="simple-dark">
                <template #button-content>
                    <bm-icon icon="3dots-v" size="lg" />
                </template>
                <bm-dropdown-item-toggle :checked="isSignatureInserted" @click="$emit('toggle-signature')">{{
                    $t("mail.compose.toolbar.insert_signature")
                }}</bm-dropdown-item-toggle>
            </bm-dropdown>
        </div>
    </div>
</template>

<script>
import { mapState } from "vuex";

import { DateComparator } from "@bluemind/date";
import { BmButton, BmIcon, BmDropdown, BmDropdownItemToggle } from "@bluemind/styleguide";

import { ComposerActionsMixin } from "~mixins";
import { MessageStatus } from "~model/message";
import { isInternalIdFaked } from "~model/draft";

export default {
    name: "MailComposerFooter",
    components: {
        BmButton,
        BmDropdown,
        BmDropdownItemToggle,
        BmIcon
    },
    mixins: [ComposerActionsMixin],
    props: {
        userPrefIsMenuBarOpened: {
            type: Boolean,
            default: false
        },
        messageKey: {
            type: String,
            required: true
        },
        signature: {
            type: String,
            required: true
        },
        isSignatureInserted: {
            type: Boolean,
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
        },
        saveMessage() {
            if (this.isSaving) {
                return this.$t("mail.draft.save.inprogress");
            } else if (this.errorOccuredOnSave) {
                return this.$t("mail.draft.save.error");
            } else if (isInternalIdFaked(this.message.remoteRef.internalId)) {
                return "";
            } else {
                return this.formattedDraftSaveDate;
            }
        },
        formattedDraftSaveDate() {
            const saveDate = this.message.date;
            if (DateComparator.isToday(saveDate)) {
                return this.$t("mail.draft.save.date.time", { time: this.$d(saveDate, "short_time") });
            }
            return this.$t("mail.draft.save.date", { date: this.$d(saveDate, "short_date") });
        },
        textFormatterLabel() {
            return this.userPrefIsMenuBarOpened
                ? this.$tc("mail.actions.textformat.hide.aria")
                : this.$tc("mail.actions.textformat.show.aria");
        }
    },
    methods: {
        openFilePicker() {
            this.$refs.attachInputRef.click();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-composer-footer {
    .toolbar .btn {
        padding: $sp-2;
    }

    display: none;

    @include media-breakpoint-up(lg) {
        display: flex;
    }
}
</style>
