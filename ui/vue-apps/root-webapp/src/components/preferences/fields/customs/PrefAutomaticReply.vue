<template>
    <div class="pref-automatic-reply">
        <bm-form-checkbox v-model="value.enabled" class="mb-3">
            {{ $t("preferences.mail.automatic_reply.activate") }}
        </bm-form-checkbox>
        <div v-if="!collapsed" :class="{ disabled: !value.enabled }">
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
            <bm-form-group :label="$t('common.message')" label-for="message" label-class="text-capitalize">
                <bm-rich-editor
                    ref="message"
                    :init-value="textHtml"
                    show-toolbar
                    has-border
                    :disabled="!value.enabled"
                    name="auto-reply"
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
import { BmFormCheckbox, BmFormGroup, BmFormInput, BmRichEditor } from "@bluemind/styleguide";
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
    computed: {
        userLang() {
            return this.$store.state.settings.lang;
        },
        isValid() {
            return !this.value.enabled || this.subject.trim() !== "";
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
        onInput(content) {
            if (content !== this.textHtml) {
                this.textHtml = content;
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-automatic-reply {
    .disabled {
        label,
        .date-range-label {
            color: $neutral-fg-disabled;
        }
    }

    .date-range {
        display: flex;
        flex-direction: column;
        gap: $sp-2;
    }
}
</style>
