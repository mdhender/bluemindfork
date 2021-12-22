<template>
    <filtered-list v-if="FOLDER_LIST_IS_FILTERED" class="flex-fill" />
    <tree-list v-else class="flex-fill" />
</template>
<script>
import { mapGetters } from "vuex";

import TreeList from "./TreeList";
import FilteredList from "./FilteredList";
import { FOLDER_LIST_IS_FILTERED } from "~/getters";

export default {
    name: "FolderList",
    components: { FilteredList, TreeList },
    computed: {
        ...mapGetters("mail", { FOLDER_LIST_IS_FILTERED })
    }
};
</script>
