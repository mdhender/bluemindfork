<template>
    <div class="text-center pr-0 d-flex flex-column">
        <div class="py-5">
            <p>
                <!-- need to be refactored with i18n component interpolation -->
                {{ $t("mail.list.search.no_result") }}<br />
                <span class="search-pattern">"{{ pattern }}"</span><br />
                {{ $t("mail.list.search.no_result.found") }}
            </p>
            <p>{{ $t("common.search.try_otherwise") }}</p>
            <p v-if="CONVERSATION_LIST_FILTERED">
                <router-link :to="$router.relative({ name: 'v:mail:home', params: { filter: null } }, $route)">
                    {{ $t("mail.list.filter.remove") }}
                </router-link>
            </p>
        </div>
        <div
            class="no-search-results-illustration"
            :style="'background: url(' + noSearchResultsIllustration + ') no-repeat right top'"
        />
    </div>
</template>

<script>
import noSearchResultsIllustration from "../../../assets/no-search-result.png";
import { mapGetters } from "vuex";
import { CONVERSATION_LIST_FILTERED } from "~/getters";

export default {
    name: "SearchResultContentEmpty",
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
    },
    computed: {
        ...mapGetters("mail", { CONVERSATION_LIST_FILTERED })
    }
};
</script>

<style lang="scss" scoped>
@import "~@bluemind/styleguide/css/variables";

.search-pattern {
    color: $primary-fg;
    font-weight: $font-weight-bold;
    word-break: break-all;
}

.no-search-results-illustration {
    flex: auto 1 1;
}
</style>
