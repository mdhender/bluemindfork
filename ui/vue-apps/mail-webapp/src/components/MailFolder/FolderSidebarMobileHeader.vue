<template>
    <bm-navbar class="folder-sidebar-mobile-header">
        <div class="flex-fill d-flex align-items-center">
            <bm-avatar :alt="displayname" />
            <div class="text-truncate px-4 large">
                {{ displayname }}
            </div>
        </div>
        <bm-icon-dropdown
            boundary="viewport"
            variant="compact-on-fill-primary"
            class="dropdown-on-fill-primary"
            size="lg"
            no-caret
            icon="3dots-vertical"
            lazy
        >
            <bm-dropdown-item-button icon="gearwheel" @click="TOGGLE_PREFERENCES()">
                {{ $t("common.preference") }}
            </bm-dropdown-item-button>
            <bm-dropdown-item to="#about">
                {{ $t("banner.about") }}
            </bm-dropdown-item>
            <bm-dropdown-item icon="power" href="/bluemind_sso_logout">
                {{ $t("banner.menu.logout") }}
            </bm-dropdown-item>
        </bm-icon-dropdown>
    </bm-navbar>
</template>

<script>
import { mapMutations } from "vuex";
import injector from "@bluemind/inject";
import { BmAvatar, BmIconDropdown, BmDropdownItemButton, BmDropdownItem, BmNavbar } from "@bluemind/ui-components";

export default {
    name: "FolderSidebarMobileHeader",
    components: { BmAvatar, BmIconDropdown, BmDropdownItemButton, BmDropdownItem, BmNavbar },
    data() {
        const userSession = injector.getProvider("UserSession").get();
        const displayname = userSession["formatedName"];

        return {
            displayname
        };
    },
    methods: {
        ...mapMutations("preferences", ["TOGGLE_PREFERENCES"])
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.folder-sidebar-mobile-header {
    .bm-avatar {
        margin: 0 base-px-to-rem(6);
    }
    padding-right: $sp-3 !important;
}
</style>
