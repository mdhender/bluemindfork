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
                variant="outline-primary"
                class="ml-2"
                :disabled="isSaving || isSending || isDeleting"
                @click.prevent="$emit('save')"
            >
                {{ $t("common.save") }}
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
        <span class="text-muted align-self-center pr-2">{{ saveMessage }}</span>
    </div>
</template>

<script>
import { BmButton } from "@bluemind/styleguide";
import { mapState } from "vuex";
import { DraftStatus } from "@bluemind/backend.mail.store";

export default {
    name: "MailMessageNewFooter",
    components: {
        BmButton
    },
    computed: {
        ...mapState("backend.mail/items", ["draft"]),
        formattedDraftSaveDate() {
            // TODO use Blandine's work when pushed
            return { value: this.draft.saveDate || new Date(), mode: "hours" };
        },
        isSending() {
            return this.draft.status == DraftStatus.SENDING;
        },
        isSaving() {
            return this.draft.status == DraftStatus.SAVING;
        },
        isDeleting() {
            return this.draft.status == DraftStatus.DELETING;
        },
        hasSaveDate() {
            return this.draft.saveDate;
        },
        saveMessage() {
            if (this.isSaving) {
                return this.$t("mail.draft.save.inprogress");
            } else if (this.hasSaveDate) {
                // TODO use Blandine's work when pushed
                if (this.formattedDraftSaveDate.mode == "hours") {
                    const time =
                        this.formattedDraftSaveDate.value.getHours() +
                        ":" +
                        this.formattedDraftSaveDate.value.getMinutes();
                    return this.$t("mail.draft.save.date.time", { time });
                } else {
                    return this.$t("mail.draft.save.date", { date: this.formattedDraftSaveDate.value });
                }
            }else{
                return null;
            }
        }
    }
};
</script>