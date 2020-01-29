<template>
    <div class="d-flex justify-content-between">
        <div>
            <bm-button
                type="submit"
                variant="primary"
                :disabled="isSending || isDeleting"
                @click.prevent="$emit('send')"
            >
                {{ $t("common.send") }}
            </bm-button>
            <bm-button
                variant="link"
                class="ml-2"
                :disabled="isSaving || isSending || isDeleting"
                @click.prevent="$emit('delete')"
            >
                {{ $t("common.delete") }}
            </bm-button>
        </div>
        <span 
            v-if="errorOccuredOnSave"
            v-bm-tooltip.bottom.ds500
            class="d-flex align-items-center pr-2 text-danger"
            :title="$t('mail.draft.save.error.reason')"
        >
            <bm-icon icon="exclamation-circle" class="mr-1" />{{ saveMessage }}
        </span>
        <span v-else class="text-muted pr-2 align-self-center">{{ saveMessage }}</span>
    </div>
</template>

<script>
import { BmButton, BmIcon, BmTooltip } from "@bluemind/styleguide";
import { DateComparator } from "@bluemind/date";
import { DraftStatus } from "@bluemind/backend.mail.store";
import { mapState } from "vuex";

export default {
    name: "MailMessageNewFooter",
    components: {
        BmButton,
        BmIcon
    },
    directives: { BmTooltip },
    computed: {
        ...mapState("mail-webapp", ["draft"]),
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
        },
        hasSaveDate() {
            return this.draft.saveDate;
        },
        saveMessage() {
            if (this.isSaving) {
                return this.$t("mail.draft.save.inprogress");
            } else if (this.errorOccuredOnSave) {
                return this.$t("mail.draft.save.error");
            } else if (this.hasSaveDate) {
                return this.formattedDraftSaveDate;
            } else {
                return null;
            }
        },
        formattedDraftSaveDate() {
            const saveDate = this.draft.saveDate || new Date();
            if (DateComparator.isToday(saveDate)) {
                return this.$t("mail.draft.save.date.time", { time: this.$d(saveDate, 'short_time') });
            }
            return this.$t("mail.draft.save.date", { date: this.$d(saveDate, 'short_date') });
        }
    }
};
</script>