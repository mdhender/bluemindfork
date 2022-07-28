<template>
    <list-collapse v-if="hasResult" :name="$t('mail.folders.groups')">
        <template v-slot:avatar>
            <mail-mailbox-icon :mailbox="GROUP_MAILBOXES[0]" class="mr-1" />
        </template>
        <div class="d-flex flex-column">
            <filtered-item v-for="folder in folders" :key="folder.key" :folder="folder" class="flex-fill" />
        </div>
        <div v-if="showMoreResultsButton" class="text-center">
            <bm-button variant="inline-secondary" @click="SHOW_MORE_FOR_GROUP_MAILBOXES">
                {{ $t("mail.folder.filter.show_more") }}
            </bm-button>
        </div>
    </list-collapse>
</template>
<script>
import { mapActions, mapGetters } from "vuex";
import { BmButton } from "@bluemind/styleguide";
import { SHOW_MORE_FOR_GROUP_MAILBOXES } from "~/actions";
import { FILTERED_GROUP_MAILBOX_RESULTS, GROUP_MAILBOXES } from "~/getters";
import MailMailboxIcon from "../MailMailboxIcon";
import FilteredItem from "./FilteredItem";
import ListCollapse from "./ListCollapse";
import { DEFAULT_LIMIT } from "../../store/folderList";

export default {
    name: "FilteredGroupMailboxes",
    components: { BmButton, FilteredItem, ListCollapse, MailMailboxIcon },
    data() {
        return { showMoreResultsButton: true };
    },
    computed: {
        ...mapGetters("mail", { FILTERED_GROUP_MAILBOX_RESULTS, GROUP_MAILBOXES }),
        hasResult() {
            return this.GROUP_MAILBOXES.length > 0 && this.FILTERED_GROUP_MAILBOX_RESULTS.length > 0;
        },
        folders() {
            return this.FILTERED_GROUP_MAILBOX_RESULTS;
        }
    },
    watch: {
        folders: {
            handler: function (value, oldValue) {
                this.showMoreResultsButton =
                    value.length % DEFAULT_LIMIT === 0 || (oldValue && oldValue.length !== value.length);
            },
            immediate: true
        }
    },
    methods: {
        ...mapActions("mail", { SHOW_MORE_FOR_GROUP_MAILBOXES })
    }
};
</script>
