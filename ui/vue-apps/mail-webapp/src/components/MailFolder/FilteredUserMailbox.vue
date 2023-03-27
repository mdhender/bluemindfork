<template>
    <list-collapse v-if="!isEmpty" :name="mailbox.name" :mailbox="mailbox">
        <div class="d-flex flex-column">
            <filtered-item v-for="folder in folders" :key="folder.key" :folder="folder" class="flex-fill" />
        </div>
        <div v-if="showMoreResultsButton" class="text-center">
            <bm-button variant="text-accent" class="w-100" @click="SHOW_MORE_FOR_USERS(mailbox)">
                {{ $t("mail.folder.filter.show_more") }}
            </bm-button>
        </div>
    </list-collapse>
</template>
<script>
import { mapActions, mapGetters } from "vuex";
import { BmButton } from "@bluemind/ui-components";
import { SHOW_MORE_FOR_USERS } from "~/actions";
import { FILTERED_USER_RESULTS } from "~/getters";
import FilteredItem from "./FilteredItem";
import ListCollapse from "./ListCollapse";
import { DEFAULT_LIMIT } from "../../store/folderList";

export default {
    name: "FilteredUserMailbox",
    components: { BmButton, FilteredItem, ListCollapse },
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
        ...mapActions("mail", { SHOW_MORE_FOR_USERS })
    }
};
</script>
