<template>
    <empty-search-result :pattern="pattern">
        <template v-if="CONVERSATION_LIST_FILTERED" #actions>
            <router-link :to="$router.relative({ name: 'v:mail:home', params: { filter: null } }, $route)">
                {{ $t("mail.list.filter.remove") }}
            </router-link>
        </template>
    </empty-search-result>
</template>

<script>
import { mapGetters } from "vuex";
import { CONVERSATION_LIST_FILTERED } from "~/getters";

import EmptySearchResult from "../EmptySearchResult";

export default {
    name: "SearchResultContentEmpty",
    components: { EmptySearchResult },
    props: {
        pattern: {
            type: String,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", { CONVERSATION_LIST_FILTERED })
    }
};
</script>
