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
        <div class="d-flex align-items-center toolbar">
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
                :aria-label="$tc('mail.actions.attach.aria')"
                :title="$tc('mail.actions.attach.aria')"
                :disabled="isSending"
                @click="openFilePicker()"
            >
                <bm-icon icon="paper-clip" size="lg" />
            </bm-button>
            <bm-dropdown v-if="hasSignature" dropup right no-caret variant="simple-dark">
                <template #button-content>
                    <bm-icon icon="3dots-v" size="lg" />
                </template>
                <bm-dropdown-item-toggle :checked="isSignatureInserted" @click="toggleSignature">{{
                    $t("mail.compose.toolbar.insert_signature")
                }}</bm-dropdown-item-toggle>
            </bm-dropdown>
        </div>
    </div>
</template>

<script>
import { DateComparator } from "@bluemind/date";
import { inject } from "@bluemind/inject";
import { BmButton, BmIcon, BmTooltip, BmDropdown, BmDropdownItemToggle } from "@bluemind/styleguide";
import { MessageStatus } from "../../model/message";
import {
    addHtmlSignature,
    addTextSignature,
    isHtmlSignaturePresent,
    isTextSignaturePresent,
    removeHtmlSignature,
    removeTextSignature
} from "../../model/signature";
import { mapMutations, mapState } from "vuex";
const USER_PREF_TEXT_ONLY = false;

export default {
    name: "MailComposerFooter",
    components: {
        BmButton,
        BmDropdown,
        BmDropdownItemToggle,
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
    data: () => ({ signature: "" }),
    computed: {
        ...mapState("session", { settings: "userSettings" }),
        ...mapState("mail", { editorContent: ({ messageCompose }) => messageCompose.editorContent }),
        hasRecipient() {
            return this.message.to.length > 0 || this.message.cc.length > 0 || this.message.bcc.length > 0;
        },
        isSending() {
            return this.message.status === MessageStatus.SENDING;
        },
        isSaving() {
            return this.message.status === MessageStatus.SAVING;
        },
        hasSignature() {
            return !!this.signature;
        },
        isSignatureInserted() {
            return (
                this.hasSignature &&
                (USER_PREF_TEXT_ONLY
                    ? isTextSignaturePresent(this.editorContent, this.signature)
                    : isHtmlSignaturePresent(this.editorContent, this.signature))
            );
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
    async created() {
        const identities = await inject("IUserMailIdentities").getIdentities();
        const defaultIdentity = identities.find(identity => identity.isDefault);
        this.signature = defaultIdentity && defaultIdentity.signature;
        if (this.signature && this.settings.insert_signature) {
            this.addSignature();
        }
    },
    mounted() {
        if (this.signature && this.settings.insert_signature) {
            this.addSignature();
        }
    },
    methods: {
        ...mapMutations("mail", ["SET_DRAFT_EDITOR_CONTENT"]),
        openFilePicker() {
            this.$refs.attachInputRef.click();
        },
        toggleSignature() {
            if (!this.isSignatureInserted) {
                this.addSignature();
            } else {
                this.removeSignature();
            }
        },
        addSignature() {
            const content = USER_PREF_TEXT_ONLY
                ? addTextSignature(this.editorContent, this.signature)
                : addHtmlSignature(this.editorContent, this.signature);
            this.SET_DRAFT_EDITOR_CONTENT(content);
        },
        removeSignature() {
            const content = USER_PREF_TEXT_ONLY
                ? removeTextSignature(this.editorContent, this.signature)
                : removeHtmlSignature(this.editorContent, this.signature);
            this.SET_DRAFT_EDITOR_CONTENT(content);
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
}
</style>
