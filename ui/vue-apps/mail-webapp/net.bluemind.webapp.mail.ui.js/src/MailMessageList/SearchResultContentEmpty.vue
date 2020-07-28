<template>
    <div class="bg-extra-light text-center h-100 pr-0">
        <div class="pt-5 font-size-lg">
            {{ $t("mail.list.search.no_result") }} <br />
            <div class="search-pattern">"{{ pattern }}"</div>
            {{ $t("mail.list.search.no_result.found") }} <br /><br />
            {{ $t("mail.list.search.no_result.try_otherwise") }}
            <div class="float-right pt-5 no-search-results-illustration w-50" v-html="noSearchResultsIllustration" />
        </div>
    </div>
</template>

<script>
import noSearchResultsIllustration from "../../assets/no-search-result.svg";

export default {
    name: "SearchResultContentEmpty",
    props: {
        pattern: {
            type: String,
            required: true
        }
    },
    data: function() {
        return {
            noSearchResultsIllustration
        };
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.font-size-lg {
    font-size: $font-size-lg;
}

.search-pattern {
    color: $info-dark;
    font-weight: $font-weight-bold;
    word-break: break-all;
}
</style>
