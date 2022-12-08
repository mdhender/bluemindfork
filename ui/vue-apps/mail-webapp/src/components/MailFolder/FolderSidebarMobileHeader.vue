<template>
    <div class="folder-sidebar-mobile-header d-lg-none d-flex pb-2 px-2 pt-5">
        <div class="flex-fill d-flex align-items-center">
            <bm-avatar :alt="displayname" />
            <div class="text-truncate pl-5 pr-2">
                {{ displayname }}
            </div>
        </div>
        <bm-icon-dropdown
            boundary="viewport"
            variant="compact-on-fill-primary"
            class="dropdown-on-fill-primary"
            size="lg"
            no-caret
            icon="3dots-v"
            lazy
        >
            <bm-dropdown-item-button icon="preferences" @click="TOGGLE_PREFERENCES()">
                {{ $t("common.preference") }}
            </bm-dropdown-item-button>
            <bm-dropdown-item to="#about">
                {{ $t("banner.about") }}
            </bm-dropdown-item>
            <bm-dropdown-item icon="sign-out" href="/bluemind_sso_logout">
                {{ $t("banner.menu.logout") }}
            </bm-dropdown-item>
        </bm-icon-dropdown>
    </div>
</template>

<script>
import { mapMutations } from "vuex";
import injector from "@bluemind/inject";
import { BmAvatar, BmIconDropdown, BmDropdownItemButton, BmDropdownItem } from "@bluemind/ui-components";

export default {
    name: "FolderSidebarMobileHeader",
    components: { BmAvatar, BmIconDropdown, BmDropdownItemButton, BmDropdownItem },
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
@import "~@bluemind/ui-components/src/css/variables";

.folder-sidebar-mobile-header {
    background-color: $fill-primary-bg-hi1 !important;
    color: $fill-primary-fg;
}
</style>
