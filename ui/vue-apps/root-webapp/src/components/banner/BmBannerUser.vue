<template>
    <bm-navbar-nav class="bm-banner-user order-0 order-lg-1 align-items-center">
        <bm-nav-item-dropdown right offset="5" class="bm-dropdown-info-dark h-100">
            <template slot="button-content">
                <bm-avatar :alt="user.displayname" class="flex-shrink-0" width="2em" />
                <span class="pr-4 username text-truncate m-auto">
                    {{ user.displayname }}
                </span>
            </template>
            <bm-dropdown-item
                v-if="canOpenSettings"
                :title="$t('banner.menu.preferences.aria')"
                @click="TOGGLE_PREFERENCES()"
            >
                <bm-label-icon icon="preferences">{{ $t("common.preference") }} </bm-label-icon>
            </bm-dropdown-item>
            <bm-dropdown-item href="/bluemind_sso_logout">
                <bm-label-icon icon="sign-out">{{ $t("banner.menu.logout") }} </bm-label-icon>
            </bm-dropdown-item>
        </bm-nav-item-dropdown>
    </bm-navbar-nav>
</template>

<script>
import { inject } from "@bluemind/inject";
import BmRoles from "@bluemind/roles";
import { BmAvatar, BmDropdownItem, BmLabelIcon, BmNavbarNav, BmNavItemDropdown } from "@bluemind/styleguide";
import { mapMutations } from "vuex";

export default {
    name: "BmBannerUser",
    components: {
        BmAvatar,
        BmDropdownItem,
        BmLabelIcon,
        BmNavbarNav,
        BmNavItemDropdown
    },
    props: {
        user: {
            required: true,
            type: Object
        }
    },
    data() {
        return { canOpenSettings: inject("UserSession").roles.includes(BmRoles.SELF_CHANGE_SETTINGS) };
    },
    methods: {
        ...mapMutations("preferences", ["TOGGLE_PREFERENCES"])
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.bm-banner-user .bm-avatar {
    position: relative;
    left: -1em;
}

.bm-banner-user .dropdown-menu {
    margin-top: 0 !important;
}

.bm-banner-user .dropdown-toggle,
.bm-banner-user .dropdown-menu {
    min-width: 10rem;
    max-width: 15rem;
}

.bm-banner-user .dropdown-toggle {
    display: flex;
    align-items: center;
    padding-left: 0 !important;
    &:after {
        position: absolute;
        right: $sp-3;
        vertical-align: middle;
    }
}

.bm-banner-user .dropdown-item {
    padding-right: $sp-3;
    padding-left: $sp-3;
}
</style>
