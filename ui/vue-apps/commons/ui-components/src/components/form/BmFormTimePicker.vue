<template>
    <bm-form-autocomplete-input
        v-model="value_"
        class="bm-form-time-picker"
        select-input-on-focus
        :disabled="disabled"
        :max-results="48"
        :selected-result="selected"
        :items="suggestions"
        :placeholder="placeholder"
        @input="onInputUpdate"
        @selected="onSelect"
    />
</template>

<script>
import BmFormAutocompleteInput from "./BmFormAutocompleteInput";

export default {
    name: "BmFormTimePicker",
    components: { BmFormAutocompleteInput },
    props: {
        value: {
            type: String,
            required: true
        },
        disabled: {
            type: Boolean,
            default: false
        },
        placeholder: {
            type: String,
            default() {
                return this.$t("common.hour");
            }
        },
        min: {
            type: String,
            default: ""
        },
        max: {
            type: String,
            default: ""
        }
    },
    data() {
        return {
            value_: "",
            selected: 0,
            suggestions: []
        };
    },
    watch: {
        value() {
            this.setInitialState();
        },
        min() {
            this.computeSuggestions();
        },
        max() {
            this.computeSuggestions();
        }
    },
    created() {
        this.computeSuggestions();
        this.setInitialState();
    },
    methods: {
        setInitialState() {
            this.value_ = this.value;
            const hasSuggestion = this.findMatchingSuggestion(this.value);
            if (hasSuggestion !== -1) {
                this.selected = hasSuggestion;
            }
        },
        computeSuggestions() {
            this.suggestions = [];
            const anyDay = "August 19, 1975 ";
            Array.from(Array(24).keys()).forEach(number => {
                const exactHour = anyDay + number + ":00";
                this.suggestions.push(this.$d(new Date(exactHour), "short_time"));
                const halfHour = anyDay + number + ":30";
                this.suggestions.push(this.$d(new Date(halfHour), "short_time"));
            });
            if (this.min) {
                this.suggestions = this.suggestions.filter(suggestion => suggestion > this.min);
            }
            if (this.max) {
                this.suggestions = this.suggestions.filter(suggestion => suggestion < this.max);
            }
        },
        onInputUpdate() {
            const hasSuggestion = this.findMatchingSuggestion(this.value_);
            if (hasSuggestion !== -1) {
                this.selected = hasSuggestion;
            }
        },
        onSelect(selected) {
            const hasSuggestion = this.findMatchingSuggestion(selected);
            if (hasSuggestion !== -1) {
                this.selected = hasSuggestion;
                this.value_ = selected;
                this.$emit("input", selected);
            }
        },
        findMatchingSuggestion(time) {
            return this.suggestions.findIndex(suggestion => suggestion.toLowerCase().includes(time.toLowerCase()));
        }
    }
};
</script>

<style lang="scss">
@import "../../css/_variables.scss";

.bm-form-time-picker input {
    text-align: center;
}
</style>
