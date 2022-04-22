<template>
    <bm-form-checkbox v-model="value" class="pref-field-check" value="true" unchecked-value="false">
        <pref-field-label :label="label" />
    </bm-form-checkbox>
</template>

<script>
import { BmFormCheckbox } from "@bluemind/styleguide";
import OneSettingField from "../mixins/OneSettingField";
import PrefFieldLabel from "./PrefFieldLabel";

export default {
    name: "PrefFieldCheck",
    components: { BmFormCheckbox, PrefFieldLabel },
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
