<template>
    <div class="search-no-results text-center my-5">
        <bm-illustration value="spider" size="md" />
        <i18n path="chooser.search.no_results">
            <template #pattern>
                <span class="search-pattern">{{ pattern }}</span>
            </template>
        </i18n>
    </div>
</template>

<script>
import { mapState } from "vuex";
import { BmIllustration } from "@bluemind/ui-components";

export default {
    name: "SearchNoResults",
    components: { BmIllustration },
    computed: {
        ...mapState("chooser", ["pattern"])
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/mixins/_responsiveness";
@import "~@bluemind/ui-components/src/css/variables";

.search-no-results {
    display: flex;
    flex-direction: column;
    gap: $sp-6;
    align-items: center;
    padding-top: $sp-7;
}
</style>
