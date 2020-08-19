<template>
    <bm-dropdown
        no-caret
        variant="inline-light"
        class="messages-options-for-mobile d-flex justify-content-end"
        v-on="$listeners"
    >
        <template v-slot:button-content><bm-icon icon="3dots" size="2x" /></template>
        <bm-dropdown-item-button v-if="messageFilter !== 'unread'" variant="dark" @click="filterUnread">
            {{ this.$t("mail.list.menu.filter") + " " + this.$t("mail.list.menu.filter.unread") }}
        </bm-dropdown-item-button>
        <bm-dropdown-divider v-if="messageFilter !== 'unread'" />
        <bm-dropdown-item-button v-if="messageFilter !== 'flagged'" variant="dark" @click="filterFlagged">
            {{ this.$t("mail.list.menu.filter") + " " + this.$t("mail.list.menu.filter.flagged") }}
        </bm-dropdown-item-button>
        <bm-dropdown-divider v-if="messageFilter !== 'flagged'" />
        <bm-dropdown-item-button v-if="messageFilter" variant="dark" @click="filterAll">
            {{ this.$t("mail.list.filter.remove") + " '" + this.$t("mail.list.menu.filter." + messageFilter) + "'" }}
        </bm-dropdown-item-button>
    </bm-dropdown></template
>
<script>
import { BmDropdown, BmDropdownItemButton, BmDropdownDivider, BmIcon } from "@bluemind/styleguide";
import { mapState } from "vuex";

export default {
    name: "MessagesOptionsForMobile",
    components: {
        BmDropdown,
        BmDropdownDivider,
        BmDropdownItemButton,
        BmIcon
    },
    computed: {
        ...mapState("mail-webapp", ["messageFilter"])
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
    z-index: 252;
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
