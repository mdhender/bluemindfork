<template>
    <filtered-list-content v-if="!FOLDER_LIST_IS_LOADING && !FOLDER_LIST_IS_EMPTY" />
</template>
<script>
import { mapGetters } from "vuex";

import FilteredListContent from "./FilteredListContent";
import { FOLDER_LIST_IS_LOADING, FOLDER_LIST_IS_EMPTY } from "~/getters";

export default {
    name: "FilteredList",
    components: { FilteredListContent },
    computed: {
        ...mapGetters("mail", { FOLDER_LIST_IS_LOADING, FOLDER_LIST_IS_EMPTY })
    }
};
</script>
