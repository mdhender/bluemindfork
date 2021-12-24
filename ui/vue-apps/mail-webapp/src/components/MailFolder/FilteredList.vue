<template>
    <filtered-list-content v-if="!FOLDER_LIST_IS_LOADING && !FOLDER_LIST_IS_EMPTY" />
    <filtered-list-empty v-else-if="!FOLDER_LIST_IS_LOADING && FOLDER_LIST_IS_EMPTY" @clearFilter="clearFilter" />
    <filtered-list-loading v-else-if="FOLDER_LIST_IS_LOADING" @clearFilter="clearFilter" />
</template>
<script>
import { mapGetters, mapMutations } from "vuex";
import { FOLDER_LIST_IS_LOADING, FOLDER_LIST_IS_EMPTY } from "~/getters";
import { SET_FOLDER_FILTER_PATTERN, SET_FOLDER_FILTER_RESULTS, RESET_FOLDER_FILTER_LIMITS } from "~/mutations";
import FilteredListContent from "./FilteredListContent";
import FilteredListEmpty from "./FilteredListEmpty";
import FilteredListLoading from "./FilteredListLoading";

export default {
    name: "FilteredList",
    components: { FilteredListContent, FilteredListEmpty, FilteredListLoading },
    computed: {
        ...mapGetters("mail", { FOLDER_LIST_IS_LOADING, FOLDER_LIST_IS_EMPTY })
    },
    methods: {
        ...mapMutations("mail", { SET_FOLDER_FILTER_PATTERN, SET_FOLDER_FILTER_RESULTS, RESET_FOLDER_FILTER_LIMITS }),
        clearFilter() {
            this.SET_FOLDER_FILTER_PATTERN(null);
            this.SET_FOLDER_FILTER_RESULTS({});
            this.RESET_FOLDER_FILTER_LIMITS();
        }
    }
};
</script>
