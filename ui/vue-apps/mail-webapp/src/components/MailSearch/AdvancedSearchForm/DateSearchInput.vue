<template>
    <div class="date-search-input">
        <bm-form-date-picker
            :locale="userLang"
            variant="outline"
            placeholder=""
            class="mobile-only"
            :value="value"
            :min="min_"
            :max="max_"
            resettable
            @input="update"
            @reset="update(null)"
        />
        <bm-form-date-picker
            :locale="userLang"
            variant="underline"
            placeholder=""
            class="desktop-only"
            :value="value"
            :min="min_"
            :max="max_"
            resettable
            @input="update"
            @reset="update(null)"
        />
    </div>
</template>

<script>
import BmFormDatePicker from "@bluemind/ui-components/src/components/form/BmFormDatePicker";

export default {
    name: "DateSearchInput",
    components: { BmFormDatePicker },
    props: {
        value: {
            type: [String, Date],
            default: ""
        },
        min: {
            type: [String, Date],
            default: ""
        },
        max: {
            type: [String, Date],
            default: ""
        }
    },
    computed: {
        userLang() {
            return this.$store.state.settings.lang;
        },
        min_() {
            return date(this.min);
        },
        max_() {
            return date(this.max);
        }
    },
    methods: {
        update(value) {
            this.$emit("update:value", value);
        }
    }
};
function date(str) {
    if (!str) {
        return null;
    }
    return new Date(str);
}
</script>
