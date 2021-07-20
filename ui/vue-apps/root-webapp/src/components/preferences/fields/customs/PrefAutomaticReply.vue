<template>
    <bm-spinner v-if="!isMailboxFilterLoaded" />
    <div v-else class="pref-automatic-reply">
        <bm-form-checkbox v-model="vacation.enabled" class="mb-3">
            {{ $t("preferences.mail.automatic_reply.activate") }}
        </bm-form-checkbox>
        <div :class="{ disabled: !vacation.enabled }">
            <bm-form-group :label="$t('mail.new.subject')" label-for="subject">
                <bm-form-input id="subject" v-model="vacation.subject" required :disabled="!vacation.enabled" />
            </bm-form-group>
            <bm-form-group :label="$t('common.message')" label-for="message" label-class="text-capitalize">
                <bm-rich-editor
                    ref="message"
                    v-model="vacation.textHtml"
                    is-menu-bar-opened
                    has-border
                    :disabled="!vacation.enabled"
                />
            </bm-form-group>

            <div class="my-3 date-range-label">{{ $t("preferences.mail.automatic_reply.period") }}</div>

            <div class="d-flex date-range">
                <bm-form-group :label="$t('common.from')" label-for="from_date" class="mr-2">
                    <bm-form-date-picker
                        id="from_date"
                        v-model="startDate"
                        :disabled="!vacation.enabled"
                        :locale="userLang"
                        value-as-date
                        show-range
                        :max="endDate"
                        :initial-date="endDate"
                    />
                </bm-form-group>
                <bm-form-group :label="$t('common.hour')" label-for="from_hour" class="mr-2">
                    <bm-form-time-picker id="from_hour" v-model="startTime" :disabled="!vacation.enabled" />
                </bm-form-group>
                <bm-button v-if="vacation.start !== null" variant="inline-secondary" @click="resetStart">
                    <bm-icon icon="trash" />
                </bm-button>

                <bm-form-group :label="$t('common.until')" label-for="to_date" class="ml-5 mr-2">
                    <bm-form-date-picker
                        id="to_date"
                        v-model="endDate"
                        :disabled="!vacation.enabled"
                        :locale="userLang"
                        value-as-date
                        show-range
                        :min="startDate"
                        :initial-date="startDate"
                    />
                </bm-form-group>
                <bm-form-group :label="$t('common.hour')" label-for="to_hour" class="mr-2">
                    <bm-form-time-picker id="to_hour" v-model="endTime" :disabled="!vacation.enabled" />
                </bm-form-group>
                <bm-button v-if="vacation.end !== null" variant="inline-secondary" @click="resetEnd">
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
import { mapMutations, mapState } from "vuex";

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
    data() {
        return {
            vacation: {
                enabled: false,
                start: null,
                end: null,
                subject: "",
                text: "",
                textHtml: ""
            },
            startDate: null,
            startTime: "",
            endDate: null,
            endTime: ""
        };
    },
    computed: {
        ...mapState("session", { userLang: ({ settings }) => settings.remote.lang }),
        ...mapState("preferences", {
            isMailboxFilterLoaded: ({ mailboxFilter }) => mailboxFilter.loaded,
            localVacation: ({ mailboxFilter }) => mailboxFilter.local.vacation
        })
    },
    watch: {
        isMailboxFilterLoaded() {
            if (this.isMailboxFilterLoaded) {
                this.init();
            }
        },
        localVacation() {
            if (JSON.stringify(this.localVacation) !== JSON.stringify(this.vacation)) {
                this.init();
            }
        },
        vacation: {
            handler() {
                this.SET_VACATION(this.vacation);
            },
            deep: true
        },
        startDate() {
            this.onStartChange();
        },
        startTime() {
            this.onStartChange();
        },
        endDate() {
            this.onEndChange();
        },
        endTime() {
            this.onEndChange();
        }
    },
    methods: {
        ...mapMutations("preferences", ["SET_VACATION"]),
        async init() {
            this.vacation = JSON.parse(JSON.stringify(this.localVacation));
            this.startDate = this.vacation.start ? new Date(this.vacation.start) : null;
            this.startTime = this.vacation.start ? this.$d(new Date(this.vacation.start), "short_time") : "";
            this.endDate = this.vacation.end ? new Date(this.vacation.end) : null;
            this.endTime = this.vacation.end ? this.$d(new Date(this.vacation.end), "short_time") : "";
            await this.$nextTick();
            this.$refs["message"].updateContent();
        },
        resetStart() {
            this.vacation.start = null;
            this.startDate = null;
            this.startTime = "";
            this.SET_VACATION(this.vacation);
        },
        resetEnd() {
            this.vacation.end = null;
            this.endDate = null;
            this.endTime = "";
            this.SET_VACATION(this.vacation);
        },
        onStartChange() {
            if (this.startTime && this.startDate) {
                const time = this.startTime.split(":"); // i18n problem ?
                const date = new Date(this.startDate);
                date.setHours(time[0]);
                date.setMinutes(time[1]);
                console.log("vacation start changed !!");
                this.vacation.start = date.getTime();
                this.SET_VACATION(this.vacation);
            }
        },
        onEndChange() {
            if (this.endTime && this.endDate) {
                const time = this.endTime.split(":"); // i18n problem ?
                const date = new Date(this.endDate);
                date.setHours(time[0]);
                date.setMinutes(time[1]);
                this.vacation.end = date.getTime();
                this.SET_VACATION(this.vacation);
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
