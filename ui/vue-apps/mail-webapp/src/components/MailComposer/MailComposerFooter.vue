<template>
    <div class="mail-composer-footer d-flex justify-content-between align-items-center">
        <div>
            <bm-button
                type="submit"
                variant="primary"
                :disabled="isSending || !hasRecipient"
                @click.prevent="$emit('send')"
            >
                {{ $t("common.send") }}
            </bm-button>
            <bm-button
                variant="simple-dark"
                class="ml-2"
                :disabled="isSaving || isSending"
                @click.prevent="$emit('delete')"
            >
                {{ $t("common.delete") }}
            </bm-button>
        </div>
        <div class="d-flex align-items-center">
            <span
                v-if="errorOccuredOnSave"
                v-bm-tooltip.bottom
                class="pr-2 text-danger"
                :title="$t('mail.draft.save.error.reason')"
            >
                <!-- trick: modify the viewBox attribute to have a correct vertical alignment -->
                <!-- eslint-disable-next-line vue/attribute-hyphenation -->
                <bm-icon icon="exclamation-circle" class="mr-1" viewBox="0 -1 12 12" />{{ saveMessage }}
            </span>
            <span v-else class="text-muted pr-2">{{ saveMessage }}</span>
            <bm-button
                v-if="!userPrefTextOnly"
                v-bm-tooltip.left
                variant="simple-dark"
                class="p-2"
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
                @change="$emit('add-attachments', $event.target.files)"
            />
            <bm-button
                v-bm-tooltip.bottom
                variant="simple-dark"
                class="p-2"
                :aria-label="$tc('mail.actions.attach.aria')"
                :title="$tc('mail.actions.attach.aria')"
                :disabled="isSending"
                @click="openFilePicker()"
            >
                <bm-icon icon="paper-clip" size="lg" />
            </bm-button>
        </div>
    </div>
</template>

<script>
import { DateComparator } from "@bluemind/date";
import { BmButton, BmIcon, BmTooltip } from "@bluemind/styleguide";

import MessageStatus from "../../store/messages/MessageStatus";

export default {
    name: "MailComposerFooter",
    components: {
        BmButton,
        BmIcon
    },
    directives: { BmTooltip },
    props: {
        userPrefTextOnly: {
            type: Boolean,
            default: false
        },
        userPrefIsMenuBarOpened: {
            type: Boolean,
            default: false
        },
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
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

<style>
.mail-composer-footer {
    z-index: 2;
    position: relative;
}
</style>
