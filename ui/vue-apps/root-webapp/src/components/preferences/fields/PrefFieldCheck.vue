<template>
    <bm-form-checkbox v-model="value" class="pref-field-check" value="true" unchecked-value="false">
        {{ label }}
    </bm-form-checkbox>
</template>

<script>
import { BmFormCheckbox } from "@bluemind/styleguide";
import OneSettingField from "../mixins/OneSettingField";

export default {
    name: "PrefFieldCheck",
    components: {
        BmFormCheckbox
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
