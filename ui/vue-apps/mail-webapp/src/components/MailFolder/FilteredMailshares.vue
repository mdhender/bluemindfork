<template>
    <list-collapse v-if="hasResult" :name="$t('common.mailshares')">
        <template v-slot:avatar>
            <mail-mailbox-icon :mailbox="MAILSHARES[0]" class="mr-1" />
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
import { SHOW_MORE_FOR_MAILSHARES } from "~/actions";
import { FILTERED_MAILSHARE_RESULTS, MAILSHARES } from "~/getters";
import MailMailboxIcon from "../MailMailboxIcon";
import FilteredItem from "./FilteredItem";
import ListCollapse from "./ListCollapse";

export default {
    name: "FilteredMailshares",
    components: { BmButton, FilteredItem, ListCollapse, MailMailboxIcon },
    data() {
        return { showMoreResultsButton: true };
    },
    computed: {
        ...mapGetters("mail", { FILTERED_MAILSHARE_RESULTS, MAILSHARES }),
        hasResult() {
            return this.MAILSHARES.length > 0 && this.FILTERED_MAILSHARE_RESULTS.length > 0;
        },
        folders() {
            return this.FILTERED_MAILSHARE_RESULTS;
        }
    },
    methods: {
        ...mapActions("mail", { SHOW_MORE_FOR_MAILSHARES }),
        async showMore() {
            const count = this.folders.length;
            await this.SHOW_MORE_FOR_MAILSHARES();
            this.showMoreResultsButton = this.folders.length > count;
        }
    }
};
</script>
