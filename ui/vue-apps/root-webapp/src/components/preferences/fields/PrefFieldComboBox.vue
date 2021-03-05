<template>
    <bm-combo-box
        v-model="input"
        :items="filteredChoices"
        :max-results="10"
        class="pref-field-combobox"
        @input="newInput => (input = newInput)"
        @selected="onSelect"
        @close="onClose"
    />
</template>

<script>
import { BmComboBox } from "@bluemind/styleguide";
import PrefFieldMixin from "../mixins/PrefFieldMixin";

export default {
    name: "PrefFieldComboBox",
    components: { BmComboBox },
    mixins: [PrefFieldMixin],
    data() {
        return { input: this.localUserSettings[this.setting] };
    },
    computed: {
        filteredChoices() {
            return this.options.choices.filter(tz => tz.includes(this.input));
        }
    },
    watch: {
        localUserSettings(value, oldValue) {
            if (value[this.setting] !== oldValue[this.setting]) {
                this.input = value[this.setting];
            }
        }
    },
    methods: {
        onClose() {
            if (!this.options.choices.find(choice => choice === this.input)) {
                this.input = this.localUserSettings[this.setting];
            }
        },
        onSelect(selected) {
            this.input = selected;
            this.localUserSettings[this.setting] = selected;
        }
    }
};
</script>

<style>
.pref-field-combobox {
    width: 4rem;
}
</style>
