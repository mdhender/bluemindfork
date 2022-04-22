<template>
    <div>
        <bm-form-checkbox
            v-model="value"
            class="pref-field-switch d-inline-block"
            :value="checkedValue"
            :unchecked-value="uncheckedValue"
            switch
        >
            <pref-field-label :label="label" />
        </bm-form-checkbox>
    </div>
</template>

<script>
import { BmFormCheckbox } from "@bluemind/styleguide";
import OneSettingField from "../mixins/OneSettingField";
import PrefFieldLabel from "./PrefFieldLabel";

export default {
    name: "PrefFieldSwitch",
    components: { BmFormCheckbox, PrefFieldLabel },
    mixins: [OneSettingField],
    props: {
        label: {
            type: [Object, String],
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
