<template>
    <div class="text-center pr-0 d-flex flex-column">
        <div class="py-4 font-size-lg">
            <p>
                {{ $t("mail.list.search.no_result") }}<br />
                <span class="search-pattern">"{{ pattern }}"</span><br />
                {{ $t("mail.list.search.no_result.found") }}
            </p>
            <p>
                {{ $t("mail.list.search.no_result.try_otherwise") }}
            </p>
        </div>
        <div
            class="no-search-results-illustration"
            :style="'background: url(' + noSearchResultsIllustration + ') no-repeat right top'"
        />
    </div>
</template>

<script>
import noSearchResultsIllustration from "~/../assets/no-search-result.png";

export default {
    name: "EmptySearchResult",
    props: {
        pattern: {
            type: String,
            required: true
        }
    },
    data: function () {
        return {
            noSearchResultsIllustration
        };
    }
};
</script>

<style lang="scss" scoped>
@import "~@bluemind/styleguide/css/variables";

.font-size-lg {
    font-size: $font-size-lg;
}

.search-pattern {
    color: $info-dark;
    font-weight: $font-weight-bold;
    word-break: break-all;
}

.no-search-results-illustration {
    flex: auto 1 1;
}
</style>
