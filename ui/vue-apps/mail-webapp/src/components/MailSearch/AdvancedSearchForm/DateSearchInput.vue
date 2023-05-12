<template>
    <div class="date-search-input">
        <bm-form-date-picker :locale="userLang" placeholder="" class="input" :value="value" @input="update" />
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
        }
    },
    computed: {
        userLang() {
            return this.$store.state.settings.lang;
        }
    },
    methods: {
        update(value) {
            this.$emit("update:value", value);
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/mixins/responsiveness";
@import "@bluemind/ui-components/src/css/variables";

.date-search-input {
    //FIXME: Remove these styles when the BmFormDatePicker will have an underline style
    @include from-lg {
        .input.b-form-datepicker {
            border-left-color: transparent !important;
            border-right-color: transparent !important;
            border-top-color: transparent !important;
            border-bottom-color: $neutral-fg-lo2 !important;
            padding-left: $sp-5 !important;
            padding-right: $sp-5 !important;
        }
    }
}
</style>
