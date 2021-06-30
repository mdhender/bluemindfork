<template>
    <div class="pref-work-hours">
        <bm-form-checkbox class="mb-2" :checked="isWholeDay" @change="isWholeDay ? setDefault() : setWholeDay()">
            {{ $t("preferences.calendar.main.whole_day") }}
        </bm-form-checkbox>
        <div class="d-inline-block mr-5">
            {{ $t("preferences.calendar.main.day_starts_at") }}
            <bm-form-timepicker
                class="mt-1"
                :max="adaptedWorkHoursEnd"
                :value="adaptedWorkHoursStart"
                :disabled="isWholeDay"
                @input="onHoursStartChanged"
            />
        </div>
        <div class="d-inline-block">
            {{ $t("preferences.calendar.main.day_ends_at") }}
            <bm-form-timepicker
                class="mt-1"
                :min="adaptedWorkHoursStart"
                :value="adaptedWorkHoursEnd"
                :disabled="isWholeDay"
                @input="onHoursEndChanged"
            />
        </div>
    </div>
</template>

<script>
import { BmFormCheckbox, BmFormTimepicker } from "@bluemind/styleguide";
import PrefFieldMixin from "../../mixins/PrefFieldMixin";

const WORK_HOURS_START_SETTING = "work_hours_start";
const WORK_HOURS_END_SETTING = "work_hours_end";

export default {
    name: "PrefWorkHours",
    components: { BmFormCheckbox, BmFormTimepicker },
    mixins: [PrefFieldMixin],
    computed: {
        workHoursEnd() {
            return this.localUserSettings[WORK_HOURS_END_SETTING];
        },
        workHoursStart() {
            return this.localUserSettings[WORK_HOURS_START_SETTING];
        },
        isWholeDay() {
            return this.workHoursEnd === "0" && this.workHoursStart === "0";
        },
        adaptedWorkHoursStart() {
            return decimalToTime(this.workHoursStart);
        },
        adaptedWorkHoursEnd() {
            return decimalToTime(this.workHoursEnd);
        }
    },
    methods: {
        setDefault() {
            this.localUserSettings[WORK_HOURS_START_SETTING] = "8";
            this.localUserSettings[WORK_HOURS_END_SETTING] = "18";
        },
        setWholeDay() {
            this.localUserSettings[WORK_HOURS_START_SETTING] = "0";
            this.localUserSettings[WORK_HOURS_END_SETTING] = "0";
        },
        onHoursStartChanged(newTime) {
            this.localUserSettings[WORK_HOURS_START_SETTING] = timeToDecimal(newTime).toString();
        },
        onHoursEndChanged(newTime) {
            this.localUserSettings[WORK_HOURS_END_SETTING] = timeToDecimal(newTime).toString();
        }
    }
};

function timeToDecimal(time) {
    let hours, minutes;
    [hours, minutes] = time.split(":").map(str => parseInt(str));
    return hours + minutes / 60;
}

function decimalToTime(decimalHours) {
    let hours, minutes;
    [hours, minutes] = decimalHours.split(".");
    if (parseInt(hours) < 10) {
        hours = "0" + hours;
    }
    minutes = minutes ? minutes * 6 : "0";
    if (parseInt(minutes) < 10) {
        minutes = "0" + minutes;
    }
    return hours + ":" + minutes;
}
</script>

<style lang="scss">
.pref-work-hours {
    div.bm-form-timepicker {
        width: 10em !important;
        &.bm-form-autocomplete-input {
            min-width: unset;
        }
        div.bm-form-input {
            width: unset !important;
        }
    }
}
</style>
