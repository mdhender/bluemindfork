<template>
    <bm-form-group :disabled="disabled">
        <template #label><pref-field-label :label="label" /></template>
        <bm-combo-box
            v-model="input"
            :items="filtered"
            :max-results="10"
            class="pref-field-combobox"
            @input="newInput => (input = newInput)"
            @selected="onSelect"
            @close="onClose"
        />
    </bm-form-group>
</template>

<script>
import { BmFormGroup, BmComboBox } from "@bluemind/styleguide";
import OneSettingField from "../mixins/OneSettingField";
import PrefFieldLabel from "./PrefFieldLabel";

export default {
    name: "PrefFieldComboBox",
    components: { BmFormGroup, BmComboBox, PrefFieldLabel },
    mixins: [OneSettingField],
    props: {
        choices: {
            type: Array,
            required: true
        },
        label: {
            type: String,
            required: false,
            default: ""
        }
    },
    data() {
        return { input: this.value };
    },
    computed: {
        filtered() {
            const input = new RegExp(this.input, "i");
            return this.choices.filter(choice => input.test(choice));
        }
    },

    watch: {
        value: {
            handler(value) {
                this.input = value;
            },
            immediate: true
        }
    },
    methods: {
        onClose() {
            if (!this.choices.includes(this.input)) {
                this.input = this.value;
            }
        },
        onSelect(selected) {
            this.value = selected;
        }
    }
};
</script>
