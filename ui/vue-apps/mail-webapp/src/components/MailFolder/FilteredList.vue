<template>
    <filtered-list-content v-if="!FOLDER_LIST_IS_LOADING && !FOLDER_LIST_IS_EMPTY" />
    <filtered-list-empty v-else-if="!FOLDER_LIST_IS_LOADING && FOLDER_LIST_IS_EMPTY" @clearFilter="RESET_FILTER" />
    <filtered-list-loading v-else-if="FOLDER_LIST_IS_LOADING" @clearFilter="RESET_FILTER" />
</template>
<script>
import { mapActions, mapGetters } from "vuex";
import { RESET_FILTER } from "~/actions";
import { FOLDER_LIST_IS_LOADING, FOLDER_LIST_IS_EMPTY } from "~/getters";
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
        ...mapActions("mail", { RESET_FILTER })
    }
};
</script>
