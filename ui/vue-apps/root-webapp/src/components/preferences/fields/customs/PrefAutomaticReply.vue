<template>
    <bm-spinner v-if="!isMailboxFilterLoaded" />
    <div v-else class="pref-automatic-reply">
        <bm-form-checkbox v-model="value.enabled" class="mb-3">
            {{ $t("preferences.mail.automatic_reply.activate") }}
        </bm-form-checkbox>
        <div v-if="!collapsed" :class="{ disabled: !value.enabled }">
            <bm-form-group
                :label="$t('mail.new.subject')"
                label-for="subject"
                :invalid-feedback="$t('preferences.mail.automatic_reply.invalid_empty_subject')"
                :state="!value.enabled || value.subject !== ''"
            >
                <bm-form-input id="subject" v-model="value.subject" required :disabled="!value.enabled" />
            </bm-form-group>
            <bm-form-group :label="$t('common.message')" label-for="message" label-class="text-capitalize">
                <bm-rich-editor
                    ref="message"
                    v-model="value.textHtml"
                    is-menu-bar-opened
                    has-border
                    :disabled="!value.enabled"
                />
            </bm-form-group>

            <div class="my-3 date-range-label">{{ $t("preferences.mail.automatic_reply.period") }}</div>

            <div class="d-flex date-range">
                <bm-form-group :label="$t('common.from')" label-for="from_date" class="mr-2">
                    <bm-form-date-picker
                        id="from_date"
                        v-model="startDate"
                        :disabled="!value.enabled"
                        :locale="userLang"
                        value-as-date
                        show-range
                        :max="endDate"
                        :initial-date="endDate"
                    />
                </bm-form-group>
                <bm-form-group :label="$t('common.hour')" label-for="from_hour" class="mr-2">
                    <bm-form-time-picker
                        id="from_hour"
                        v-model="startTime"
                        :disabled="!(value.enabled && value.start)"
                    />
                </bm-form-group>
                <bm-button v-if="value.start !== null" variant="inline-secondary" @click="value.start = null">
                    <bm-icon icon="trash" />
                </bm-button>

                <bm-form-group :label="$t('common.until')" label-for="to_date" class="ml-5 mr-2">
                    <bm-form-date-picker
                        id="to_date"
                        v-model="endDate"
                        :disabled="!value.enabled"
                        :locale="userLang"
                        value-as-date
                        show-range
                        :min="startDate"
                        :initial-date="startDate"
                    />
                </bm-form-group>
                <bm-form-group :label="$t('common.hour')" label-for="to_hour" class="mr-2">
                    <bm-form-time-picker id="to_hour" v-model="endTime" :disabled="!(value.enabled && value.end)" />
                </bm-form-group>
                <bm-button v-if="value.end !== null" variant="inline-secondary" @click="value.end = null">
                    <bm-icon icon="trash" />
                </bm-button>
            </div>
        </div>
    </div>
</template>

<script>
import {
    BmButton,
    BmFormCheckbox,
    BmFormDatePicker,
    BmFormGroup,
    BmFormInput,
    BmFormTimePicker,
    BmIcon,
    BmRichEditor,
    BmSpinner
} from "@bluemind/styleguide";
import { mapState } from "vuex";
import CentralizedSaving from "../../mixins/CentralizedSaving";

export default {
    name: "PrefAutomaticReply",
    components: {
        BmButton,
        BmFormCheckbox,
        BmFormDatePicker,
        BmFormGroup,
        BmFormInput,
        BmFormTimePicker,
        BmIcon,
        BmRichEditor,
        BmSpinner
    },
    mixins: [CentralizedSaving],
    data() {
        return {
            value: {
                enabled: false,
                start: null,
                end: null,
                subject: "",
                text: "",
                textHtml: ""
            }
        };
    },
    computed: {
        ...mapState("session", { userLang: ({ settings }) => settings.remote.lang }),
        ...mapState("preferences", {
            isMailboxFilterLoaded: ({ mailboxFilter }) => mailboxFilter.loaded
        }),
        isValid() {
            return !this.value.enabled || this.value.subject.trim() !== "";
        },
        startDate: {
            get() {
                return this.value.start ? new Date(this.value.start) : null;
            },
            set(value) {
                const date = new Date(value);
                if (this.value.start) {
                    const old = new Date(this.value.start);
                    date.setHours(old.getHours());
                    date.setMinutes(old.getMinutes());
                }
                this.value.start = date.getTime();
            }
        },
        startTime: {
            get() {
                return this.value.start ? this.$d(new Date(this.value.start), "short_time") : "";
            },
            set(value) {
                if (this.value.start) {
                    const date = new Date(this.value.start);
                    const [hours, minutes] = value.split(":"); // i18n problem ?
                    date.setHours(hours);
                    date.setMinutes(minutes);
                    this.value.start = date.getTime();
                }
            }
        },
        endDate: {
            get() {
                return this.value.end ? new Date(this.value.end) : null;
            },
            set(value) {
                const date = new Date(value);
                if (this.value.end) {
                    const old = new Date(this.value.end);
                    date.setHours(old.getHours());
                    date.setMinutes(old.getMinutes());
                }
                this.value.end = date.getTime();
            }
        },
        endTime: {
            get() {
                return this.value.end ? this.$d(new Date(this.value.end), "short_time") : "";
            },
            set(value) {
                if (this.value.end) {
                    const date = new Date(this.value.end);
                    const [hours, minutes] = value.split(":"); // i18n problem ?
                    date.setHours(hours);
                    date.setMinutes(minutes);
                    this.value.end = date.getTime();
                }
            }
        }
    },
    watch: {
        isMailboxFilterLoaded() {
            if (this.isMailboxFilterLoaded) {
                this.init();
            }
        },
        value: {
            async handler(value, old) {
                if (value.textHtml !== old.textHtml) {
                    await this.$nextTick();
                    this.$refs["message"]?.updateContent();
                }
            },
            deep: true
        }
    },
    created() {
        const save = async ({ state: { current, saved }, dispatch }) => {
            if (current && !current.options.saved) {
                try {
                    await dispatch("preferences/SAVE_MAILBOX_FILTER", { vacation: current.value }, { root: true });
                    this.PUSH_STATE({ value: current.value, options: { saved: true } });
                } catch {
                    this.PUSH_STATE(saved);
                }
            }
        };
        this.registerSaveAction(save);
    },
    methods: {
        async init() {
            this.value = { ...this.$store.state.preferences.mailboxFilter.remote.vacation };
            await this.$nextTick();
            this.$refs["message"].updateContent();
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
            color: $alternate-light;
        }
    }
    .date-range {
        .bm-form-date-picker,
        .bm-form-time-picker {
            width: 7rem !important;
            min-width: unset;
        }
        .bm-form-input {
            width: unset !important;
        }
    }
}
</style>
