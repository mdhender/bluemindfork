<template>
    <bm-icon-dropdown
        no-caret
        variant="compact-on-fill-primary"
        size="lg"
        class="messages-options-for-mobile w-100"
        icon="3dots-v"
        v-on="$listeners"
        @hide="showSortOptions ? $event.preventDefault() : undefined"
    >
        <template v-if="showSortOptions">
            <bm-dropdown-header id="sort-header-label">
                {{ this.$t("mail.list.sort.title") }}
            </bm-dropdown-header>
            <bm-dropdown-item-button
                v-for="(option, index) in sorts"
                :key="index"
                :active="sort.field === option.field && sort.order === option.order"
                aria-describedby="sort-header-label"
                @click="
                    showSortOptions = false;
                    sort = option;
                "
            >
                {{ $t(`mail.list.sort.fields.${option.field}.${option.order}`) }}
            </bm-dropdown-item-button>
        </template>
        <template v-else>
            <bm-dropdown-item-button @click="showSortOptions = true">
                {{ this.$t("mail.list.menu.sort") }}
            </bm-dropdown-item-button>
            <bm-dropdown-divider />
            <bm-dropdown-item-button v-if="!CONVERSATION_LIST_UNREAD_FILTER_ENABLED" @click="filterUnread">
                {{ this.$t("mail.list.menu.filter") + " " + this.$t("mail.list.menu.filter.unread") }}
            </bm-dropdown-item-button>
            <bm-dropdown-item-button v-if="!CONVERSATION_LIST_FLAGGED_FILTER_ENABLED" @click="filterFlagged">
                {{ this.$t("mail.list.menu.filter") + " " + this.$t("mail.list.menu.filter.flagged") }}
            </bm-dropdown-item-button>
            <template v-if="CONVERSATION_LIST_FILTERED">
                <bm-dropdown-divider />
                <bm-dropdown-item-button @click="filterAll">
                    {{ this.$t("mail.list.filter.remove") + " '" + this.$t("mail.list.menu.filter." + filter) + "'" }}
                </bm-dropdown-item-button>
            </template>
        </template>
    </bm-icon-dropdown>
</template>
<script>
import { BmIconDropdown, BmDropdownItemButton, BmDropdownDivider, BmDropdownHeader } from "@bluemind/ui-components";
import { mapGetters, mapState } from "vuex";
import { SET_CONVERSATION_LIST_SORT } from "~/mutations";
import { SortField, SortOrder } from "~/store/conversationList";

import {
    CONVERSATION_LIST_UNREAD_FILTER_ENABLED,
    CONVERSATION_LIST_FLAGGED_FILTER_ENABLED,
    CONVERSATION_LIST_FILTERED
} from "~/getters";

export default {
    name: "MessagesOptionsForMobile",
    components: {
        BmIconDropdown,
        BmDropdownDivider,
        BmDropdownHeader,
        BmDropdownItemButton
    },
    data() {
        return {
            showSortOptions: false,
            sorts: Object.values(SortField).flatMap(field => Object.values(SortOrder).map(order => ({ field, order })))
        };
    },
    computed: {
        ...mapGetters("mail", {
            CONVERSATION_LIST_UNREAD_FILTER_ENABLED,
            CONVERSATION_LIST_FLAGGED_FILTER_ENABLED,
            CONVERSATION_LIST_FILTERED
        }),
        ...mapState("mail", { filter: state => state.conversationList.filter }),
        sort: {
            get() {
                return this.$store.state.mail.conversationList.sort;
            },
            set(value) {
                this.$store.commit(`mail/${SET_CONVERSATION_LIST_SORT}`, value);
            }
        }
    },
    methods: {
        filterUnread() {
            const path = this.$router.relative({ name: "v:mail:home", params: { filter: "unread" } }, this.$route);
            this.$router.push(path);
        },
        filterFlagged() {
            const path = this.$router.relative({ name: "v:mail:home", params: { filter: "flagged" } }, this.$route);
            this.$router.push(path);
        },
        filterAll() {
            const path = this.$router.relative({ name: "v:mail:home", params: { filter: null } }, this.$route);
            this.$router.push(path);
        }
    }
};
</script>
<style lang="scss">
.messages-options-for-mobile {
    .dropdown-divider {
        margin: 0.05rem 0;
    }
    .dropdown-menu {
        box-shadow: none;
        position: fixed !important;
        top: auto !important;
        bottom: 0px !important;
        transform: none !important;
        right: 0px !important;
        line-height: 2;
    }
}
</style>
