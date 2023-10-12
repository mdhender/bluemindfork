<template>
    <bm-form-input
        class="pref-search-input"
        autofocus
        :variant="variant"
        :size="variant === 'outline' ? 'sm' : 'md'"
        :value="search"
        :placeholder="$t('common.search')"
        icon="search"
        :resettable="resettable"
        left-icon
        :aria-label="$t('common.search')"
        autocomplete="off"
        @input="SET_SEARCH"
        @reset="SET_SEARCH('')"
    />
</template>

<script>
import { BmFormInput } from "@bluemind/ui-components";
import { mapMutations, mapState } from "vuex";

export default {
    name: "PrefSearchInput",
    components: { BmFormInput },
    props: {
        variant: {
            type: String,
            default: "outline",
            validator: function (value) {
                return ["outline", "inline-on-fill-primary"].includes(value);
            }
        },
        resettable: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        ...mapState("preferences", ["search"])
    },
    methods: {
        ...mapMutations("preferences", ["SET_SEARCH"])
    }
};
</script>
