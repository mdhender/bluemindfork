<template>
    <list-collapse v-if="hasResult" :name="$t('common.mailshares')">
        <filtered-item v-for="folder in folders" :key="folder.key" :folder="folder" class="flex-fill" />
    </list-collapse>
</template>
<script>
import FilteredItem from "./FilteredItem";
import ListCollapse from "./ListCollapse.vue";

export default {
    name: "FilteredMailshares",
    components: {
        FilteredItem,
        ListCollapse
    },
    computed: {
        hasResult() {
            // TODO: connect to store;
            return this.$store.getters[`mail/MAILSHARES`].length > 0;
        },
        folders() {
            // TODO: connect to store;
            return this.$store.getters[`mail/MAILSHARE_FOLDERS`];
        }
    }
};
</script>
