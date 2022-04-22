<template>
    <bm-form-group :disabled="disabled">
        <template #label><pref-field-label :label="label" /></template>
        <bm-form-multi-select
            v-model="selected"
            class="pref-field-multi-select"
            :options="choices"
            :auto-collapse="true"
            :placeholder="placeholder"
            :select-all="selectAll"
            :select-all-label="selectAllLabel"
        />
    </bm-form-group>
</template>

<script>
import { BmFormMultiSelect, BmFormGroup } from "@bluemind/styleguide";
import OneSettingField from "../mixins/OneSettingField";
import PrefFieldLabel from "./PrefFieldLabel";

export default {
    name: "PrefFieldMultiSelect",
    components: { BmFormMultiSelect, BmFormGroup, PrefFieldLabel },
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
        },
        placeholder: {
            type: String,
            required: false,
            default: ""
        },
        selectAll: {
            type: Boolean,
            required: false,
            default: true
        },
        selectAllLabel: {
            type: String,
            required: false,
            default: undefined
        }
    },
    computed: {
        selected: {
            get() {
                return this.value?.split(",").filter(Boolean) || [];
            },
            set(value) {
                this.value = value.join(",");
            }
        }
    }
};
</script>
