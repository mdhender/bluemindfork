<template>
    <div class="pref-event-reminder">
        <bm-form-checkbox class="mb-1" :checked="isReminderSet" @change="isReminderSet ? remove() : setDefault()">
            {{ $t("preferences.calendar.main.default_reminder") }}
        </bm-form-checkbox>
        <bm-form-input-number
            :disabled="!isReminderSet"
            min="1"
            :value="settingWithUnit.value.toString()"
            class="align-middle mr-3"
            @input="newValue => onSettingChanged({ newValue })"
        />
        <bm-form-select
            :disabled="!isReminderSet"
            :value="settingWithUnit.unitMultiplicator"
            :options="selectOptions"
            @input="newUnitMultiplicator => onSettingChanged({ newUnitMultiplicator })"
        />
    </div>
</template>

<script>
import { BmFormCheckbox, BmFormInputNumber, BmFormSelect } from "@bluemind/styleguide";
import PrefFieldMixin from "../../mixins/PrefFieldMixin";

const SECONDS_PER_MINUTE = 60;
const SECONDS_PER_HOUR = SECONDS_PER_MINUTE * 60;
const SECONDS_PER_DAY = SECONDS_PER_HOUR * 24;

const SECONDS_FOR_DEFAULT_REMINDER = 15 * SECONDS_PER_MINUTE;

export default {
    name: "PrefEventReminder",
    components: { BmFormCheckbox, BmFormInputNumber, BmFormSelect },
    mixins: [PrefFieldMixin],
    data() {
        return {
            selectOptions: [
                { text: this.$t("common.seconds"), value: 1 },
                { text: this.$t("common.minutes"), value: SECONDS_PER_MINUTE },
                { text: this.$t("common.hours"), value: SECONDS_PER_HOUR },
                { text: this.$t("common.days"), value: SECONDS_PER_DAY }
            ]
        };
    },
    computed: {
        settingInSeconds() {
            return this.localUserSettings[this.setting];
        },
        isReminderSet() {
            return !!this.settingInSeconds;
        },
        settingWithUnit() {
            return findBestTimeUnit(this.settingInSeconds);
        }
    },
    methods: {
        setDefault() {
            this.save(SECONDS_FOR_DEFAULT_REMINDER);
        },
        onSettingChanged({ newUnitMultiplicator, newValue }) {
            const unitMultiplicator = newUnitMultiplicator
                ? newUnitMultiplicator
                : this.settingWithUnit.unitMultiplicator;
            const value = newValue ? newValue : this.settingWithUnit.value;
            this.save(value * unitMultiplicator);
        },
        save(seconds) {
            this.localUserSettings[this.setting] = seconds;
        },
        remove() {
            this.save("");
        }
    }
};

function findBestTimeUnit(seconds) {
    if (seconds % SECONDS_PER_DAY === 0) {
        return { unitMultiplicator: SECONDS_PER_DAY, value: Math.floor(seconds / SECONDS_PER_DAY) };
    } else if (seconds % SECONDS_PER_HOUR === 0) {
        return { unitMultiplicator: SECONDS_PER_HOUR, value: Math.floor(seconds / SECONDS_PER_HOUR) };
    } else if (seconds % SECONDS_PER_MINUTE === 0) {
        return { unitMultiplicator: SECONDS_PER_MINUTE, value: Math.floor(seconds / SECONDS_PER_MINUTE) };
    }
    return { unitMultiplicator: 1, value: seconds };
}
</script>

<style lang="scss">
.pref-event-reminder {
    div.bm-form-select {
        width: unset !important;
    }
    .bm-form-input-number {
        display: inline-flex !important;
        width: 10em;
    }
}
</style>
