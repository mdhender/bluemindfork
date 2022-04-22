<template>
    <bm-form-group :disabled="disabled">
        <template #label><pref-field-label :label="label" /></template>
        <bm-form-input v-model="value" class="pref-field-input" />
    </bm-form-group>
</template>

<script>
import { BmFormGroup, BmFormInput } from "@bluemind/styleguide";
import OneSettingField from "../mixins/OneSettingField";
import PrefFieldLabel from "./PrefFieldLabel";

export default {
    name: "PrefFieldInput",
    components: {
        BmFormGroup,
        BmFormInput,
        PrefFieldLabel
    },
    mixins: [OneSettingField],
    props: {
        label: {
            type: [Object, String],
            required: false,
            default: ""
        }
    }
};
</script>
