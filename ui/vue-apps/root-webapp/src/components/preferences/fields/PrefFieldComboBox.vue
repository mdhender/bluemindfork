<template>
    <bm-form-group :aria-label="label" :label="label" :disabled="disabled">
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

export default {
    name: "PrefFieldComboBox",
    components: { BmFormGroup, BmComboBox },
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

<style>
.pref-field-combobox {
    width: 4rem;
}
</style>
