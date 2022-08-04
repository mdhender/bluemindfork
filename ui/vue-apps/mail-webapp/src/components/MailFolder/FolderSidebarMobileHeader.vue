<template>
    <div class="folder-sidebar-mobile-header d-lg-none d-flex pb-2 px-2 pt-5">
        <div class="flex-fill d-flex align-items-center">
            <bm-avatar :alt="displayname" />
            <div class="text-wrap px-3">
                {{ displayname }}
            </div>
        </div>
        <bm-contextual-menu boundary="viewport" variant="inline-on-fill-primary" class="bm-dropdown-fill-primary-hi1">
            <bm-dropdown-item-button icon="preferences" @click="TOGGLE_PREFERENCES()">
                {{ $t("common.preference") }}
            </bm-dropdown-item-button>
            <bm-dropdown-item to="#about">
                {{ $t("banner.about") }}
            </bm-dropdown-item>
            <bm-dropdown-item icon="sign-out" href="/bluemind_sso_logout">
                {{ $t("banner.menu.logout") }}
            </bm-dropdown-item>
        </bm-contextual-menu>
    </div>
</template>

<script>
import { mapMutations } from "vuex";
import injector from "@bluemind/inject";
import { BmAvatar, BmContextualMenu, BmDropdownItemButton, BmDropdownItem } from "@bluemind/styleguide";

export default {
    name: "FolderSidebarMobileHeader",
    components: { BmAvatar, BmContextualMenu, BmDropdownItemButton, BmDropdownItem },
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
@import "@bluemind/styleguide/css/_variables.scss";

.folder-sidebar-mobile-header {
    background-color: $fill-primary-bg-hi1 !important;
    color: $fill-primary-fg;

    .bm-avatar {
        width: 3.5em !important;
        height: 3.5em !important;
    }
}
</style>
