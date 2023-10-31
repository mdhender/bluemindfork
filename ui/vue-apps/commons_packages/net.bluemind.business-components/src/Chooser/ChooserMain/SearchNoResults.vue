<template>
    <div class="search-no-results text-center">
        <bm-illustration value="spider" size="md" over-background />
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
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.search-no-results {
    display: flex;
    flex-direction: column;
    gap: $sp-6;
    align-items: center;
    padding-top: $sp-7;
    cursor: default;
}
</style>
