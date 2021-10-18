<template>
    <bm-form-group :aria-label="label" :label="label" :disabled="disabled">
        <bm-form-input v-model="value" class="pref-field-input" />
    </bm-form-group>
</template>

<script>
import { BmFormGroup, BmFormInput } from "@bluemind/styleguide";
import OneSettingField from "../mixins/OneSettingField";

export default {
    name: "PrefFieldInput",
    components: {
        BmFormGroup,
        BmFormInput
    },
    mixins: [OneSettingField],
    props: {
        label: {
            type: String,
            required: false,
            default: ""
        }
    }
};
</script>
