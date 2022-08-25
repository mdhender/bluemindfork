<template>
    <bm-icon-dropdown
        no-caret
        variant="compact-on-fill-primary"
        size="lg"
        class="messages-options-for-mobile w-100"
        icon="3dots-v"
        v-on="$listeners"
    >
        <bm-dropdown-item-button v-if="!CONVERSATION_LIST_UNREAD_FILTER_ENABLED" @click="filterUnread">
            {{ this.$t("mail.list.menu.filter") + " " + this.$t("mail.list.menu.filter.unread") }}
        </bm-dropdown-item-button>
        <bm-dropdown-divider v-if="!CONVERSATION_LIST_UNREAD_FILTER_ENABLED" />
        <bm-dropdown-item-button v-if="!CONVERSATION_LIST_FLAGGED_FILTER_ENABLED" @click="filterFlagged">
            {{ this.$t("mail.list.menu.filter") + " " + this.$t("mail.list.menu.filter.flagged") }}
        </bm-dropdown-item-button>
        <bm-dropdown-divider v-if="!CONVERSATION_LIST_UNREAD_FILTER_ENABLED" />
        <bm-dropdown-item-button v-if="CONVERSATION_LIST_FILTERED" @click="filterAll">
            {{ this.$t("mail.list.filter.remove") + " '" + this.$t("mail.list.menu.filter." + filter) + "'" }}
        </bm-dropdown-item-button>
    </bm-icon-dropdown>
</template>
<script>
import { BmIconDropdown, BmDropdownItemButton, BmDropdownDivider } from "@bluemind/styleguide";
import { mapGetters, mapState } from "vuex";
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
        BmDropdownItemButton
    },
    computed: {
        ...mapGetters("mail", {
            CONVERSATION_LIST_UNREAD_FILTER_ENABLED,
            CONVERSATION_LIST_FLAGGED_FILTER_ENABLED,
            CONVERSATION_LIST_FILTERED
        }),
        ...mapState("mail", { filter: state => state.conversationList.filter })
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
