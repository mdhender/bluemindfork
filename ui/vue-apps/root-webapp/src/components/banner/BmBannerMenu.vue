<template>
    <bm-navbar-nav class="bm-banner-user order-0 order-lg-1 align-self-stretch align-items-center">
        <bm-nav-item-dropdown right offset="5" class="h-100" :class="{ offline: !isOnline }">
            <template slot="button-content">
                <bm-avatar :alt="user.displayname" class="flex-shrink-0" width="2em" :status="status" />
                <span class="pr-4 username text-truncate m-auto">
                    {{ user.displayname }}
                </span>
            </template>
            <bm-dropdown-item
                v-if="canOpenSettings"
                :title="$t('banner.menu.preferences.aria')"
                icon="preferences"
                @click="TOGGLE_PREFERENCES()"
            >
                {{ $t("common.preference") }}
            </bm-dropdown-item>
            <bm-dropdown-item icon="blue-mind" to="#about">
                {{ $t("banner.about") }}
            </bm-dropdown-item>
            <bm-dropdown-item v-if="canLogout" icon="sign-out" href="/bluemind_sso_logout">
                {{ $t("banner.menu.logout") }}
            </bm-dropdown-item>
        </bm-nav-item-dropdown>
    </bm-navbar-nav>
</template>

<script>
import { inject } from "@bluemind/inject";
import BmRoles from "@bluemind/roles";
import { BmAvatar, BmDropdownItem, BmNavbarNav, BmNavItemDropdown } from "@bluemind/styleguide";
import { green, white } from "@bluemind/styleguide/css/exports/colors.scss";

import { mapMutations, mapState } from "vuex";

export default {
    name: "BmBannerMenu",
    components: {
        BmAvatar,
        BmDropdownItem,
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
        const userSession = inject("UserSession");
        return {
            canOpenSettings: userSession.roles.includes(BmRoles.SELF_CHANGE_SETTINGS),
            canLogout: Boolean(userSession.userId)
        };
    },
    computed: {
        ...mapState("root-app", ["isOnline"]),
        status() {
            if (this.isOnline) {
                return { color: green, label: this.$t("common.status.online") };
            } else {
                return { color: white, label: this.$t("common.status.offline") };
            }
        }
    },
    methods: {
        ...mapMutations("preferences", ["TOGGLE_PREFERENCES"])
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";
$contrasted-color: color-yiq(theme-color("info-dark")) !important;

.bm-banner-user {
    .bm-avatar {
        position: relative;
        left: -1em;
    }

    .dropdown-menu,
    .dropdown-toggle,
    .dropdown {
        background-color: theme-color-level("info-dark", 4) !important;
        color: $contrasted-color;
        text-decoration: none;
        &:active,
        &:visited,
        &:hover,
        &:focus,
        &:hover::before {
            text-decoration: none;
            color: $primary !important;
            background-color: theme-color-level("info-dark", 4) !important;
        }
    }

    .dropdown.offline,
    .dropdown.offline .dropdown-toggle {
        background-color: $gray-900 !important;
        &:active,
        &:visited,
        &:hover,
        &:focus,
        &:hover::before {
            color: $contrasted-color !important;
            background-color: $gray-900 !important;
        }
    }

    .dropdown-item {
        color: $contrasted-color;
        &:focus,
        &:hover,
        &:active,
        &:active:focus {
            background-color: unset;
            color: $primary !important;
        }
    }

    .dropdown-menu {
        margin-top: 0 !important;
    }

    .dropdown-toggle,
    .dropdown-menu {
        min-width: 10rem;
        max-width: 15rem;
    }

    .dropdown-toggle {
        display: flex;
        align-items: center;
        padding-left: 0 !important;
        &:after {
            position: absolute;
            right: $sp-3;
            vertical-align: middle;
        }
    }
}
</style>
