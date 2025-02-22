<template>
    <div class="pref-event-reminder pb-3">
        <bm-form-checkbox class="mb-3" :checked="isReminderSet" @change="isReminderSet ? remove() : setDefault()">
            {{ $t("preferences.calendar.main.default_reminder") }}
        </bm-form-checkbox>
        <bm-form-input-number
            :disabled="!isReminderSet"
            show-buttons
            min="1"
            :value="settingWithUnit.value.toString()"
            class="align-middle mr-5"
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
import { SECONDS_PER_DAY, SECONDS_PER_HOUR, SECONDS_PER_MINUTE } from "@bluemind/date";
import { BmFormCheckbox, BmFormInputNumber, BmFormSelect } from "@bluemind/ui-components";
import OneSettingField from "../../mixins/OneSettingField";

const SECONDS_FOR_DEFAULT_REMINDER = 15 * SECONDS_PER_MINUTE;

export default {
    name: "PrefEventReminder",
    components: { BmFormCheckbox, BmFormInputNumber, BmFormSelect },
    mixins: [OneSettingField],
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
        isReminderSet() {
            return !!this.value;
        },
        settingWithUnit() {
            return findBestTimeUnit(this.value);
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
            this.value = seconds;
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
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-event-reminder {
    div.bm-form-select {
        width: unset !important;
    }
    .bm-form-input-number {
        display: inline-flex !important;
        width: base-px-to-rem(160) !important;
    }
}
</style>
