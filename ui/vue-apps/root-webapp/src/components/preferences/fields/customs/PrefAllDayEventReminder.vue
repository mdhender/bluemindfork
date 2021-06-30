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
import { BmFormCheckbox, BmFormSelect, BmFormTimepicker } from "@bluemind/styleguide";
import PrefFieldMixin from "../../mixins/PrefFieldMixin";

const SECONDS_PER_MINUTE = 60;
const SECONDS_PER_HOUR = SECONDS_PER_MINUTE * 60;
const SECONDS_PER_DAY = SECONDS_PER_HOUR * 24;

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
            if (!this.settingInSeconds) {
                return {
                    timeSelected: "09:00",
                    daysBefore: 1
                };
            }
            const completeDays = Math.trunc(this.settingInSeconds / SECONDS_PER_DAY);
            if (completeDays <= MAX_DAYS_SUGGESTED) {
                const secondsLeft = this.settingInSeconds - SECONDS_PER_DAY * completeDays;
                const completeHours = secondsLeft / SECONDS_PER_HOUR;
                const decimalHour = 24 - completeHours;

                let hourInSeconds = decimalHour * SECONDS_PER_HOUR;
                const fullHours = Math.floor(hourInSeconds / SECONDS_PER_HOUR);
                hourInSeconds = hourInSeconds - fullHours * SECONDS_PER_HOUR;
                const fullMinutes = Math.floor(hourInSeconds / SECONDS_PER_MINUTE);
                hourInSeconds = hourInSeconds - fullMinutes * SECONDS_PER_MINUTE;

                const anyDay = "August 19, 1975 ";
                const date = anyDay + fullHours + ":" + fullMinutes;

                return { timeSelected: this.$d(new Date(date), "short_time"), daysBefore: completeDays + 1 };
            } else {
                // FIXME: what do we display if we dont find matching "daysBefore" ?
                console.error("unable to decompose " + this.settingInSeconds + " seconds for all_day fields...");
                return {};
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
