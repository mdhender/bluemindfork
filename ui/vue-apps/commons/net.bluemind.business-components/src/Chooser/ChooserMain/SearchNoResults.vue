<template>
    <div class="search-no-results text-center my-5">
        <p class="m-0">
            <i18n path="chooser.search.no_results">
                <template #pattern>
                    <span class="search-pattern">{{ pattern }}</span>
                </template>
            </i18n>
        </p>
        <img :src="emptyResultsIllustration" />
    </div>
</template>

<script>
import { mapState } from "vuex";
import emptyResultsIllustration from "../../../assets/empty-search-results.png";

export default {
    name: "SearchNoResults",
    data() {
        return { emptyResultsIllustration };
    },
    computed: {
        ...mapState("chooser", ["pattern"])
    }
};
</script>
