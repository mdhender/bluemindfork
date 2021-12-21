<template>
    <list-collapse v-if="hasResult" :name="mailbox.name">
        <filtered-item v-for="folder in folders" :key="folder.key" :folder="folder" class="flex-fill" />
    </list-collapse>
</template>
<script>
import FilteredItem from "./FilteredItem";
import ListCollapse from "./ListCollapse.vue";

export default {
    name: "FilteredUserMailbox",
    components: {
        FilteredItem,
        ListCollapse
    },
    props: {
        mailbox: {
            type: Object,
            required: true
        }
    },
    computed: {
        hasResult() {
            // TODO: connect to store;
            return true;
        },
        folders() {
            // TODO: connect to store;
            return this.$store.getters[`mail/MAILBOX_FOLDERS`](this.mailbox);
        }
    }
};
</script>
