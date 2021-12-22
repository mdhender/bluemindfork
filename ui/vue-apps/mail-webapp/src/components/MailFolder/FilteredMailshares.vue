<template>
    <list-collapse v-if="hasResult" :name="$t('common.mailshares')">
        <template v-slot:avatar>
            <mail-mailbox-icon :mailbox="MAILSHARES[0]" class="mr-1" />
        </template>
        <div class="d-flex flex-column">
            <filtered-item v-for="folder in folders" :key="folder.key" :folder="folder" class="flex-fill" />
        </div>
    </list-collapse>
</template>
<script>
import { mapGetters } from "vuex";
import MailMailboxIcon from "../MailMailboxIcon";
import FilteredItem from "./FilteredItem";
import ListCollapse from "./ListCollapse.vue";
import { FILTERED_MAILSHARE_RESULTS, MAILSHARES } from "~/getters";

export default {
    name: "FilteredMailshares",
    components: {
        FilteredItem,
        ListCollapse,
        MailMailboxIcon
    },
    computed: {
        ...mapGetters("mail", { FILTERED_MAILSHARE_RESULTS, MAILSHARES }),
        hasResult() {
            return this.MAILSHARES.length > 0 && this.FILTERED_MAILSHARE_RESULTS.length > 0;
        },
        folders() {
            return this.FILTERED_MAILSHARE_RESULTS;
        }
    }
};
</script>
