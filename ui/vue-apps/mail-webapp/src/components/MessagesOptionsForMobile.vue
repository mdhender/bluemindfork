<template>
    <bm-dropdown
        no-caret
        variant="inline-light"
        class="messages-options-for-mobile d-flex justify-content-end"
        v-on="$listeners"
    >
        <template v-slot:button-content><bm-icon icon="3dots" size="2x" /></template>
        <bm-dropdown-item-button v-if="!MESSAGE_LIST_UNREAD_FILTER_ENABLED" variant="dark" @click="filterUnread">
            {{ this.$t("mail.list.menu.filter") + " " + this.$t("mail.list.menu.filter.unread") }}
        </bm-dropdown-item-button>
        <bm-dropdown-divider v-if="!MESSAGE_LIST_UNREAD_FILTER_ENABLED" />
        <bm-dropdown-item-button v-if="!MESSAGE_LIST_FLAGGED_FILTER_ENABLED" variant="dark" @click="filterFlagged">
            {{ this.$t("mail.list.menu.filter") + " " + this.$t("mail.list.menu.filter.flagged") }}
        </bm-dropdown-item-button>
        <bm-dropdown-divider v-if="!MESSAGE_LIST_UNREAD_FILTER_ENABLED" />
        <bm-dropdown-item-button v-if="MESSAGE_LIST_FILTERED" variant="dark" @click="filterAll">
            {{ this.$t("mail.list.filter.remove") + " '" + this.$t("mail.list.menu.filter." + filter) + "'" }}
        </bm-dropdown-item-button>
    </bm-dropdown>
</template>
<script>
import { BmDropdown, BmDropdownItemButton, BmDropdownDivider, BmIcon } from "@bluemind/styleguide";
import { mapGetters, mapState } from "vuex";
import {
    MESSAGE_LIST_UNREAD_FILTER_ENABLED,
    MESSAGE_LIST_FLAGGED_FILTER_ENABLED,
    MESSAGE_LIST_FILTERED
} from "~getters";

export default {
    name: "MessagesOptionsForMobile",
    components: {
        BmDropdown,
        BmDropdownDivider,
        BmDropdownItemButton,
        BmIcon
    },
    computed: {
        ...mapGetters("mail", {
            MESSAGE_LIST_UNREAD_FILTER_ENABLED,
            MESSAGE_LIST_FLAGGED_FILTER_ENABLED,
            MESSAGE_LIST_FILTERED
        }),
        ...mapState("mail", { filter: state => state.messageList.filter })
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
