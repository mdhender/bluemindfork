<template>
    <div class="pref-work-hours">
        <bm-form-checkbox class="mb-2" :checked="isWholeDay" @change="isWholeDay ? setDefault() : setWholeDay()">
            {{ $t("preferences.calendar.main.whole_day") }}
        </bm-form-checkbox>
        <div class="d-inline-block mr-5">
            {{ $t("preferences.calendar.main.day_starts_at") }}
            <bm-form-time-picker
                class="mt-1"
                :max="adaptedWorkHoursEnd"
                :value="displayedStart"
                :disabled="isWholeDay"
                @input="onHoursStartChanged"
            />
        </div>
        <div class="d-inline-block">
            {{ $t("preferences.calendar.main.day_ends_at") }}
            <bm-form-time-picker
                class="mt-1"
                :min="adaptedWorkHoursStart"
                :value="displayedEnd"
                :disabled="isWholeDay"
                @input="onHoursEndChanged"
            />
        </div>
    </div>
</template>

<script>
import { BmFormCheckbox, BmFormTimePicker } from "@bluemind/styleguide";
import MultipleSettingsField from "../../mixins/MultipleSettingsField";

const WORK_HOURS_START_SETTING = "work_hours_start";
const WORK_HOURS_END_SETTING = "work_hours_end";

export default {
    name: "PrefWorkHours",
    components: { BmFormCheckbox, BmFormTimePicker },
    mixins: [MultipleSettingsField],
    props: {
        settings: {
            type: Array,
            required: false,
            default: () => [WORK_HOURS_START_SETTING, WORK_HOURS_END_SETTING]
        }
    },
    data() {
        return { tmpStart: null, tmpEnd: null };
    },
    computed: {
        workHoursEnd() {
            return this.value[WORK_HOURS_END_SETTING];
        },
        workHoursStart() {
            return this.value[WORK_HOURS_START_SETTING];
        },
        isWholeDay() {
            return this.workHoursEnd === "0" && this.workHoursStart === "0";
        },
        adaptedWorkHoursStart() {
            return decimalToTime(this.workHoursStart);
        },
        adaptedWorkHoursEnd() {
            return decimalToTime(this.workHoursEnd);
        },
        displayedStart() {
            return this.workHoursStart === "0" && this.tmpStart
                ? decimalToTime(this.tmpStart)
                : this.adaptedWorkHoursStart;
        },
        displayedEnd() {
            return this.workHoursEnd === "0" && this.tmpEnd ? decimalToTime(this.tmpEnd) : this.adaptedWorkHoursEnd;
        }
    },
    methods: {
        setDefault() {
            this.value[WORK_HOURS_START_SETTING] = this.tmpStart ? this.tmpStart : "8";
            this.value[WORK_HOURS_END_SETTING] = this.tmpEnd ? this.tmpEnd : "18";
        },
        setWholeDay() {
            this.tmpStart = this.value[WORK_HOURS_START_SETTING];
            this.tmpEnd = this.value[WORK_HOURS_END_SETTING];
            this.value[WORK_HOURS_START_SETTING] = "0";
            this.value[WORK_HOURS_END_SETTING] = "0";
        },
        onHoursStartChanged(newTime) {
            this.value[WORK_HOURS_START_SETTING] = timeToDecimal(newTime).toString();
        },
        onHoursEndChanged(newTime) {
            this.value[WORK_HOURS_END_SETTING] = timeToDecimal(newTime).toString();
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
    div.bm-form-time-picker {
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
