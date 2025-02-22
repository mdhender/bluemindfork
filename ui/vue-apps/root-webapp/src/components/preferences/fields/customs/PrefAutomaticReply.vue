<template>
    <div class="pref-automatic-reply">
        <bm-form-checkbox v-model="value.enabled" class="mb-3">
            {{ $t("preferences.mail.automatic_reply.activate") }}
        </bm-form-checkbox>
        <div :class="{ disabled: !value.enabled }">
            <bm-form-group
                :label="$t('common.subject')"
                label-for="subject"
                :invalid-feedback="$t('preferences.mail.automatic_reply.invalid_empty_subject')"
                :state="subjectInputState"
            >
                <bm-form-input
                    id="subject"
                    v-model="value.subject"
                    required
                    :disabled="!value.enabled"
                    :state="subjectInputState"
                />
            </bm-form-group>
            <bm-form-group
                :label="$t('common.message')"
                label-for="message"
                label-class="text-capitalize"
                :invalid-feedback="invalidText"
                :state="!isTooHeavy"
            >
                <bm-rich-editor
                    ref="message"
                    :init-value="textHtml"
                    :dark-mode="IS_COMPUTED_THEME_DARK"
                    show-toolbar
                    has-border
                    :disabled="!value.enabled"
                    :default-font-family="composerDefaultFont"
                    :extra-font-families="EXTRA_FONT_FAMILIES"
                    name="auto-reply"
                    :invalid="isTooHeavy"
                    @input="onInput"
                />
            </bm-form-group>

            <div class="d-flex date-range">
                <pref-automatic-reply-optional-date
                    v-model="value.start"
                    :disabled="!value.enabled"
                    :labels="{
                        main: $t('common.from_date'),
                        nullDate: $t('common.now'),
                        day: $t('preferences.mail.automatic_reply.start_date'),
                        time: $t('preferences.mail.automatic_reply.start_time')
                    }"
                />
                <pref-automatic-reply-optional-date
                    v-model="value.end"
                    :disabled="!value.enabled"
                    :min="value.start"
                    :labels="{
                        main: $t('common.until'),
                        nullDate: $t('common.indefinitely'),
                        day: $t('preferences.mail.automatic_reply.end_date'),
                        time: $t('preferences.mail.automatic_reply.end_time')
                    }"
                />
            </div>
        </div>
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import { inject } from "@bluemind/inject";
import { computeUnit } from "@bluemind/file-utils";
import { BmFormCheckbox, BmFormGroup, BmFormInput, BmRichEditor } from "@bluemind/ui-components";
import PrefAutomaticReplyOptionalDate from "./PrefAutomaticReplyOptionalDate.vue";
import CentralizedSaving from "../../mixins/CentralizedSaving";

export default {
    name: "PrefAutomaticReply",
    components: {
        BmFormCheckbox,
        BmFormGroup,
        BmFormInput,
        BmRichEditor,
        PrefAutomaticReplyOptionalDate
    },
    mixins: [CentralizedSaving],
    data: () => {
        return {
            maxSize: null,
            isTooHeavy: false
        };
    },
    computed: {
        ...mapGetters("settings", ["IS_COMPUTED_THEME_DARK", "EXTRA_FONT_FAMILIES"]),
        composerDefaultFont() {
            return this.$store.state.settings.composer_default_font;
        },
        userLang() {
            return this.$store.state.settings.lang;
        },
        isValid() {
            return !this.value.enabled || (this.subject.trim() !== "" && !this.isTooHeavy);
        },
        subjectInputState() {
            if (!this.value.enabled) {
                return null;
            }
            return this.value.subject !== "";
        },
        textHtml: {
            get() {
                return this.value.textHtml || "";
            },
            set(value) {
                this.value.textHtml = value;
            }
        },
        subject: {
            get() {
                return this.value.subject || "";
            },
            set(value) {
                this.value.subject = value;
            }
        },
        invalidText() {
            return this.$t("mail.message.max_size", { size: computeUnit(this.maxSize, this.$i18n) });
        }
    },
    watch: {
        value: {
            async handler(value, old) {
                if (value.textHtml !== old?.textHtml) {
                    this.$refs["message"]?.setContent(this.textHtml);
                }
            },
            deep: true
        }
    },
    created() {
        const save = async ({ state: { current }, dispatch }) => {
            await dispatch("preferences/SAVE_VACATION", current.value, { root: true });
        };
        this.registerSaveAction(save);

        this.value = { ...this.$store.state.preferences.mailboxFilter.vacation };
    },
    methods: {
        async onInput(content) {
            if (content !== this.textHtml) {
                this.maxSize = await this.getMaxSize();
                this.isTooHeavy = new Blob([content]).size > this.maxSize;
                this.textHtml = content;
            }
        },
        async getMaxSize() {
            let maxSize = this.maxSize;
            if (!maxSize) {
                const { userId } = inject("UserSession");
                const config = await inject("MailboxesPersistence").getMailboxConfig(userId);
                maxSize = config.messageMaxSize / 2;
            }
            // Out of office is a response message, we arbitrarily divide by two the max size of this message to take into account the previous message
            return maxSize;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-automatic-reply {
    max-width: base-px-to-rem(700);

    .disabled {
        label,
        .date-range-label {
            color: $neutral-fg-disabled;
        }
    }

    .date-range {
        margin-top: $sp-6;
        display: flex;
        flex-direction: column;
        gap: $sp-2;
    }
}
</style>
