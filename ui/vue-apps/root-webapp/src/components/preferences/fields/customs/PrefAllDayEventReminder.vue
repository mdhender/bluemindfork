<template>
    <div class="pref-all-day-default-event-reminder pb-3">
        <bm-form-checkbox class="mb-3" :checked="isReminderSet" @change="isReminderSet ? remove() : setDefault()">
            {{ $t("preferences.calendar.main.default_allday_reminder") }}
        </bm-form-checkbox>
        <bm-form-time-picker
            :value="decomposedSetting.timeSelected"
            :disabled="!isReminderSet"
            class="align-middle mr-5"
            @input="newTime => onSettingChanged({ newTime })"
        />
        <bm-form-select
            :value="decomposedSetting.daysBefore"
            :disabled="!isReminderSet"
            :options="selectOptions"
            @input="newDaysBefore => onSettingChanged({ newDaysBefore })"
        />
    </div>
</template>

<script>
import { SECONDS_PER_DAY, SECONDS_PER_HOUR } from "@bluemind/date";
import { BmFormCheckbox, BmFormSelect, BmFormTimePicker } from "@bluemind/ui-components";
import OneSettingField from "../../mixins/OneSettingField";

const SECONDS_FOR_ALL_DAY_REMINDER = (24 - 9) * SECONDS_PER_HOUR; // "09:00", 1 day before
const MAX_DAYS_SUGGESTED = 6;

export default {
    name: "PrefAllDayEventReminder",
    components: { BmFormCheckbox, BmFormSelect, BmFormTimePicker },
    mixins: [OneSettingField],
    data() {
        return {
            selectOptions: Array.from(Array(MAX_DAYS_SUGGESTED).keys()).map(daysBefore => ({
                text: this.$tc("common.days_before", daysBefore + 1, { count: daysBefore + 1 }),
                value: daysBefore + 1
            }))
        };
    },
    computed: {
        isReminderSet() {
            return !!this.value;
        },
        decomposedSetting() {
            const defaultTimeSelected = this.$d(new Date(null, null, null, 9, 0), "short_time");
            if (!this.value) {
                return { timeSelected: defaultTimeSelected, daysBefore: 1 };
            }
            const completeDays = Math.trunc(this.value / SECONDS_PER_DAY);
            if (completeDays <= MAX_DAYS_SUGGESTED) {
                const date = new Date(null, null, null, 0, 0, -this.value);
                return { timeSelected: this.$d(date, "short_time"), daysBefore: completeDays + 1 };
            } else {
                // eslint-disable-next-line no-console
                console.warn(
                    `unable to decompose ${this.value} seconds for all_day fields, display default values instead.`
                );
                return { timeSelected: defaultTimeSelected, daysBefore: MAX_DAYS_SUGGESTED };
            }
        }
    },
    methods: {
        setDefault() {
            this.save(SECONDS_FOR_ALL_DAY_REMINDER);
        },
        onSettingChanged({ newTime, newDaysBefore }) {
            const time = newTime ? newTime : this.decomposedSetting.timeSelected;
            const daysBefore = newDaysBefore ? newDaysBefore : this.decomposedSetting.daysBefore;

            let hours, minutes;
            [hours, minutes] = time.split(":").map(str => parseInt(str));
            const decimalHours = hours + minutes / 60;
            const secondsBeforeNextDay = (24 - decimalHours) * SECONDS_PER_HOUR;
            this.save(secondsBeforeNextDay + (daysBefore - 1) * SECONDS_PER_DAY);
        },
        save(seconds) {
            this.value = seconds;
        },
        remove() {
            this.save("");
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-all-day-default-event-reminder {
    div.bm-form-select,
    div.bm-form-input {
        width: unset !important;
    }
    div.bm-form-time-picker {
        display: inline-block !important;
        width: base-px-to-rem(160) !important;
        &.bm-form-autocomplete-input {
            min-width: unset;
        }
    }
}
</style>
