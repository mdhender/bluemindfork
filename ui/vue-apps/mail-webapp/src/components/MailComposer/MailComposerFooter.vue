<template>
    <div class="mail-composer-footer d-flex justify-content-between align-items-center">
        <div>
            <bm-button
                type="submit"
                variant="primary"
                :disabled="isSending || isDeleting || !hasRecipient"
                @click.prevent="$emit('send')"
            >
                {{ $t("common.send") }}
            </bm-button>
            <bm-button
                variant="simple-dark"
                class="ml-2"
                :disabled="isSaving || isSending || isDeleting"
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
                :disabled="isSending || isDeleting"
                @click="$emit('toggleTextFormat')"
            >
                <bm-icon icon="text-format" size="lg" />
            </bm-button>
            <input ref="attachInputRef" type="file" multiple hidden @change="addAttachments($event.target.files)" />
            <bm-button
                v-bm-tooltip.bottom
                variant="simple-dark"
                class="p-2"
                :aria-label="$tc('mail.actions.attach.aria')"
                :title="$tc('mail.actions.attach.aria')"
                :disabled="isSending || isDeleting"
                @click="openFilePicker()"
            >
                <bm-icon icon="paper-clip" size="lg" />
            </bm-button>
        </div>
    </div>
</template>

<script>
import { BmButton, BmIcon, BmTooltip } from "@bluemind/styleguide";
import { DateComparator } from "@bluemind/date";
import { DraftStatus } from "@bluemind/backend.mail.store";
import { mapState, mapActions, mapGetters } from "vuex";

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
        }
    },
    computed: {
        ...mapState("mail-webapp", ["draft"]),
        ...mapGetters("mail-webapp/draft", ["hasRecipient"]),
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
        ...mapActions("mail-webapp", ["addAttachments", "saveDraft"]),
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
