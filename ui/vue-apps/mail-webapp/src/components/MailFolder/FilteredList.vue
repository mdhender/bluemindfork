<template>
    <filtered-list-content v-if="!FOLDER_LIST_IS_LOADING && !FOLDER_LIST_IS_EMPTY" />
    <filtered-list-empty v-else-if="!FOLDER_LIST_IS_LOADING && FOLDER_LIST_IS_EMPTY" />
</template>
<script>
import { mapGetters } from "vuex";

import FilteredListContent from "./FilteredListContent";
import FilteredListEmpty from "./FilteredListEmpty";
import { FOLDER_LIST_IS_LOADING, FOLDER_LIST_IS_EMPTY } from "~/getters";

export default {
    name: "FilteredList",
    components: { FilteredListContent, FilteredListEmpty },
    computed: {
        ...mapGetters("mail", { FOLDER_LIST_IS_LOADING, FOLDER_LIST_IS_EMPTY })
    }
};
</script>
