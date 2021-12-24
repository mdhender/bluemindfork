<template>
    <list-collapse v-if="!isEmpty" :name="mailbox.name">
        <template v-slot:avatar>
            <mail-mailbox-icon :mailbox="mailbox" class="mr-1" />
        </template>
        <div class="d-flex flex-column">
            <filtered-item v-for="folder in folders" :key="folder.key" :folder="folder" class="flex-fill" />
        </div>
        <div v-if="showMoreResultsButton" class="text-center">
            <bm-button variant="inline-primary" @click="showMore">
                {{ $t("mail.folder.filter.show_more") }}
            </bm-button>
        </div>
    </list-collapse>
</template>
<script>
import { mapActions, mapGetters } from "vuex";
import { BmButton } from "@bluemind/styleguide";
import { SHOW_MORE_FOR_USERS } from "~/actions";
import { FILTERED_USER_RESULTS } from "~/getters";
import MailMailboxIcon from "../MailMailboxIcon";
import FilteredItem from "./FilteredItem";
import ListCollapse from "./ListCollapse";

export default {
    name: "FilteredUserMailbox",
    components: { BmButton, FilteredItem, ListCollapse, MailMailboxIcon },
    props: {
        mailbox: {
            type: Object,
            required: true
        }
    },
    data() {
        return { showMoreResultsButton: true };
    },
    computed: {
        ...mapGetters("mail", { FILTERED_USER_RESULTS }),
        isEmpty() {
            return this.folders.length === 0;
        },
        folders() {
            return this.FILTERED_USER_RESULTS[this.mailbox.key];
        }
    },
    methods: {
        ...mapActions("mail", { SHOW_MORE_FOR_USERS }),
        async showMore() {
            const count = this.folders.length;
            await this.SHOW_MORE_FOR_USERS(this.mailbox);
            this.showMoreResultsButton = this.folders.length > count;
        }
    }
};
</script>
