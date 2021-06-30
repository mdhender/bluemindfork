<template>
    <div class="pref-all-day-default-event-reminder">
        <bm-form-checkbox class="mb-1" :checked="isReminderSet" @change="isReminderSet ? remove() : setDefault()">
            {{ $t("preferences.calendar.main.default_allday_reminder") }}
        </bm-form-checkbox>
        <bm-form-timepicker
            :value="decomposedSetting.timeSelected"
            :disabled="!isReminderSet"
            class="align-middle mr-3"
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
import { BmFormCheckbox, BmFormSelect, BmFormTimepicker } from "@bluemind/styleguide";
import PrefFieldMixin from "../../mixins/PrefFieldMixin";

const SECONDS_FOR_ALL_DAY_REMINDER = (24 - 9) * SECONDS_PER_HOUR; // "09:00", 1 day before
const MAX_DAYS_SUGGESTED = 6;

export default {
    name: "PrefAllDayEventReminder",
    components: { BmFormCheckbox, BmFormSelect, BmFormTimepicker },
    mixins: [PrefFieldMixin],
    data() {
        return {
            selectOptions: Array.from(Array(MAX_DAYS_SUGGESTED).keys()).map(daysBefore => ({
                text: this.$tc("common.days_before", daysBefore + 1, { count: daysBefore + 1 }),
                value: daysBefore + 1
            }))
        };
    },
    computed: {
        settingInSeconds() {
            return this.localUserSettings[this.setting];
        },
        isReminderSet() {
            return !!this.settingInSeconds;
        },
        decomposedSetting() {
            const defaultTimeSelected = this.$d(new Date(null, null, null, 9, 0), "short_time");
            if (!this.settingInSeconds) {
                return { timeSelected: defaultTimeSelected, daysBefore: 1 };
            }
            const completeDays = Math.trunc(this.settingInSeconds / SECONDS_PER_DAY);
            if (completeDays <= MAX_DAYS_SUGGESTED) {
                const date = new Date(null, null, null, 0, 0, -this.settingInSeconds);
                return { timeSelected: this.$d(date, "short_time"), daysBefore: completeDays + 1 };
            } else {
                console.error(
                    "unable to decompose " +
                        this.settingInSeconds +
                        " seconds for all_day fields, display default values instead."
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
            this.localUserSettings[this.setting] = seconds;
        },
        remove() {
            this.save("");
        }
    }
};
</script>

<style lang="scss">
.pref-all-day-default-event-reminder {
    div.bm-form-select,
    div.bm-form-input {
        width: unset !important;
    }
    div.bm-form-timepicker {
        display: inline-block !important;
        width: 10em !important;
        &.bm-form-autocomplete-input {
            min-width: unset;
        }
    }
}
</style>
