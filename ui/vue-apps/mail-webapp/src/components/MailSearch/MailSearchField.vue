<template>
    <bm-form-input
        ref="input"
        :value="value"
        class="mail-search-field flex-fill"
        variant="underline"
        :placeholder="$t('common.search')"
        icon="search"
        resettable
        left-icon
        :size="size"
        :aria-label="$t('common.search')"
        autocomplete="off"
        @input="val => $emit('update:value', val)"
        @focus="$emit('active')"
        @reset="$emit('reset')"
        @keydown.enter="$emit('search')"
    />
</template>

<script>
import { BmFormInput } from "@bluemind/ui-components";

export default {
    name: "MailSearchField",
    components: { BmFormInput },
    props: {
        value: {
            type: String,
            default: ""
        },
        size: {
            type: String,
            default: "sm"
        }
    },
    methods: {
        blur() {
            this.$refs.input.blur();
        }
    }
};
</script>

<style lang="scss">
.mail-search-field > input {
    border: none !important;
}
</style>
