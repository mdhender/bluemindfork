<template>
    <bm-form-input
        ref="input"
        :value="currentSearch.pattern"
        class="mail-search-input flex-fill"
        :variant="variant"
        :placeholder="noPlaceholder ? null : $t('common.action.search')"
        :icon="resettable ? 'search' : null"
        :resettable="resettable"
        left-icon
        :size="size"
        :aria-label="$t('common.search')"
        autocomplete="off"
        @input="setPattern"
        @focus="$emit('active')"
        @reset="$emit('reset')"
    />
</template>

<script>
import { mapMutations, mapState } from "vuex";
import { BmFormInput } from "@bluemind/ui-components";
import { SET_CURRENT_SEARCH_PATTERN } from "~/mutations";

export default {
    name: "MailSearchInput",
    components: { BmFormInput },
    props: {
        value: {
            type: String,
            default: ""
        },
        size: {
            type: String,
            default: "sm"
        },
        variant: {
            type: String,
            default: "underline"
        },
        resettable: {
            type: Boolean,
            default: false
        },
        noPlaceholder: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        ...mapState("mail", {
            currentSearch: ({ conversationList }) => conversationList.search.currentSearch
        })
    },
    methods: {
        ...mapMutations("mail", { SET_CURRENT_SEARCH_PATTERN }),
        blur() {
            this.$refs.input.blur();
        },
        focus() {
            this.$refs.input.focus();
        },
        setPattern(pattern) {
            this.SET_CURRENT_SEARCH_PATTERN(pattern);
        }
    }
};
</script>
