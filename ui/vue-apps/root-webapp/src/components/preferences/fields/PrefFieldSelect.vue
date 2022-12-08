<template>
    <bm-form-group :disabled="disabled">
        <template #label><pref-field-label :label="label" /></template>
        <bm-form-select v-model="value" class="pref-field-select" :options="choices" :auto-min-width="false" />
    </bm-form-group>
</template>

<script>
import { BmFormGroup, BmFormSelect } from "@bluemind/ui-components";
import OneSettingField from "../mixins/OneSettingField";
import PrefFieldLabel from "./PrefFieldLabel";

export default {
    name: "PrefFieldSelect",
    components: {
        BmFormGroup,
        BmFormSelect,
        PrefFieldLabel
    },
    mixins: [OneSettingField],
    props: {
        choices: {
            type: Array,
            required: true
        },
        label: {
            type: [Object, String],
            required: false,
            default: ""
        }
    }
};
</script>
