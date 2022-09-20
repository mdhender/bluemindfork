<template>
    <div class="pref-working-days">
        <div>{{ $t("preferences.calendar.main.working_days") }}</div>
        <div class="choices">
            <bm-form-checkbox
                v-for="choice in choices"
                :key="choice.value"
                v-model="selectedChoices[choice.value]"
                :class="{ 'move-first': choice.value === 'sun' && sundayFirst }"
                @change="updateValue()"
                >{{ choice.text }}</bm-form-checkbox
            >
        </div>
    </div>
</template>

<script>
import { BmFormCheckbox } from "@bluemind/styleguide";
import OneSettingField from "../../mixins/OneSettingField";

export default {
    name: "PrefWorkingDays",
    components: { BmFormCheckbox },
    mixins: [OneSettingField],
    props: {
        choices: {
            type: Array,
            required: true
        }
    },
    data: function () {
        return {
            selectedChoices: []
        };
    },
    computed: {
        sundayFirst() {
            const field = this.$store.state.preferences.fields["calendar.main.view.day_weekstart"];
            return field?.current?.value === "sunday";
        }
    },
    created() {
        for (const choice of this.choices) {
            this.selectedChoices[choice.value] = false;
        }
        for (const day of this.value.split(",").filter(Boolean)) {
            this.selectedChoices[day] = true;
        }
    },
    methods: {
        updateValue() {
            let newValue = "";
            for (const key in this.selectedChoices) {
                if (this.selectedChoices[key]) {
                    newValue += key + ",";
                }
            }
            if (newValue.endsWith(",")) {
                newValue = newValue.substring(0, newValue.length - 1);
            }
            this.value = newValue;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-working-days {
    .choices {
        display: flex;
        flex-wrap: wrap;
        gap: $sp-4 $sp-6;
        margin: $sp-4 0;

        .move-first {
            order: -1;
        }
    }
}
</style>
