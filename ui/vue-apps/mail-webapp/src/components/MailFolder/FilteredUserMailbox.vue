<template>
    <list-collapse v-if="!isEmpty" :name="mailbox.name">
        <template v-slot:avatar>
            <mail-mailbox-icon :mailbox="mailbox" class="mr-1" />
        </template>
        <div class="d-flex flex-column">
            <filtered-item v-for="folder in folders" :key="folder.key" :folder="folder" class="flex-fill" />
        </div>
    </list-collapse>
</template>
<script>
import { mapGetters } from "vuex";
import { FILTERED_USER_RESULTS } from "~/getters";
import MailMailboxIcon from "../MailMailboxIcon";
import FilteredItem from "./FilteredItem";
import ListCollapse from "./ListCollapse";

export default {
    name: "FilteredUserMailbox",
    components: {
        FilteredItem,
        ListCollapse,
        MailMailboxIcon
    },
    props: {
        mailbox: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", { FILTERED_USER_RESULTS }),
        isEmpty() {
            return this.folders.length === 0;
        },
        folders() {
            return this.FILTERED_USER_RESULTS[this.mailbox.key];
        }
    }
};
</script>
