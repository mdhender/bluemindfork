<template>
    <bm-form-input
        :value="search"
        class="pref-search-input"
        :placeholder="$t('common.search')"
        icon="search"
        resettable
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
    computed: {
        ...mapState("preferences", ["search"])
    },
    methods: {
        ...mapMutations("preferences", ["SET_SEARCH"])
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-search-input {
    @include until-lg {
        background-color: $surface;
    }
}
</style>
