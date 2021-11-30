<template>
    <bm-form-checkbox
        v-model="value"
        class="pref-field-switch d-inline-block"
        :value="checkedValue"
        :unchecked-value="uncheckedValue"
        switch
    >
        {{ label }}
    </bm-form-checkbox>
</template>

<script>
import { BmFormCheckbox } from "@bluemind/styleguide";
import OneSettingField from "../mixins/OneSettingField";

export default {
    name: "PrefFieldSwitch",
    components: { BmFormCheckbox },
    mixins: [OneSettingField],
    props: {
        label: {
            type: String,
            required: false,
            default: ""
        },
        checkedValue: {
            type: String,
            required: false,
            default: "true"
        },
        uncheckedValue: {
            type: String,
            required: false,
            default: "false"
        }
    }
};
</script>
