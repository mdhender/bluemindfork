<template>
    <list-collapse v-if="hasResult" :name="$t('common.mailshares')" :mailbox="MAILSHARES[0]">
        <div class="d-flex flex-column">
            <filtered-item v-for="folder in folders" :key="folder.key" :folder="folder" class="flex-fill" />
        </div>
        <div v-if="showMoreResultsButton" class="text-center">
            <bm-button variant="text-accent" class="w-100" @click="SHOW_MORE_FOR_MAILSHARES">
                {{ $t("mail.folder.filter.show_more") }}
            </bm-button>
        </div>
    </list-collapse>
</template>
<script>
import { mapActions, mapGetters } from "vuex";
import { BmButton } from "@bluemind/ui-components";
import { SHOW_MORE_FOR_MAILSHARES } from "~/actions";
import { FILTERED_MAILSHARE_RESULTS, MAILSHARES } from "~/getters";
import FilteredItem from "./FilteredItem";
import ListCollapse from "./ListCollapse";
import { DEFAULT_LIMIT } from "../../store/folderList";

export default {
    name: "FilteredMailshares",
    components: { BmButton, FilteredItem, ListCollapse },
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
    watch: {
        folders: {
            handler: function (value, oldValue) {
                this.showMoreResultsButton = oldValue?.length !== value.length && value.length % DEFAULT_LIMIT === 0;
            },
            immediate: true
        }
    },
    methods: {
        ...mapActions("mail", { SHOW_MORE_FOR_MAILSHARES })
    }
};
</script>
