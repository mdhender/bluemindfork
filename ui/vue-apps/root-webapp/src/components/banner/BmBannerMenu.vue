<template>
    <bm-navbar-nav
        class="bm-banner-user order-0 order-lg-1 align-self-stretch align-items-stretch"
        :class="{ offline: !isOnline }"
    >
        <bm-nav-item v-if="!logged" :href="loginUrl" class="flex-fill">
            <bm-avatar :alt="user.displayname" size="sm" :status="status" />
            <span class="username text-truncate w-100 text-center">
                {{ $t("common.log_in") }}
            </span>
        </bm-nav-item>
        <bm-nav-item-dropdown right offset="5" class="flex-fill">
            <template v-if="logged" slot="button-content">
                <bm-avatar :alt="user.displayname" :urn="user.urn" size="sm" :status="status" />
                <span class="username text-truncate m-auto">
                    {{ user.displayname }}
                </span>
            </template>
            <bm-dropdown-item
                v-if="canOpenSettings"
                :title="$t('banner.menu.preferences.aria')"
                icon="gearwheel"
                @click="TOGGLE_PREFERENCES()"
            >
                {{ $t("common.preference") }}
            </bm-dropdown-item>
            <bm-dropdown-item icon="bluemind" to="#about">
                {{ $t("banner.about") }}
            </bm-dropdown-item>
            <bm-dropdown-item v-if="logged" icon="power" href="/bluemind_sso_logout">
                {{ $t("banner.menu.logout") }}
            </bm-dropdown-item>
            <bm-dropdown-item v-else icon="user" :href="loginUrl">
                {{ $t("common.log_in") }}
            </bm-dropdown-item>
        </bm-nav-item-dropdown>
    </bm-navbar-nav>
</template>

<script>
import { inject } from "@bluemind/inject";
import BmRoles from "@bluemind/roles";
import { BmAvatar, BmDropdownItem, BmNavbarNav, BmNavItem, BmNavItemDropdown } from "@bluemind/ui-components";

import { mapMutations, mapState } from "vuex";

export default {
    name: "BmBannerMenu",
    components: {
        BmAvatar,
        BmDropdownItem,
        BmNavbarNav,
        BmNavItem,
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
            logged: Boolean(userSession.userId)
        };
    },
    computed: {
        ...mapState("root-app", ["isOnline"]),
        status() {
            if (this.isOnline) {
                return { color: "var(--fill-success-bg)", label: this.$t("common.status.online") };
            } else {
                return { color: "var(--neutral-bg)", label: this.$t("common.status.offline") };
            }
        },
        loginUrl() {
            return `?askedUri=${this.$router.options.base}${this.$route.fullPath}`;
        }
    },
    methods: {
        ...mapMutations("preferences", ["TOGGLE_PREFERENCES"])
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "~@bluemind/ui-components/src/css/utils/variables";

.bm-banner-user {
    max-width: base-px-to-rem(240);
    .bm-avatar {
        position: absolute;
        left: math.div(-$avatar-width-sm, 2);
    }
    .dropdown-menu,
    .nav-link,
    .dropdown {
        background-color: $fill-primary-bg-hi1 !important;
        color: $fill-primary-fg !important;
        text-decoration: none;
        &:active,
        &:hover,
        &:focus,
        &:hover::before {
            text-decoration: none;
            color: $fill-primary-fg-hi1 !important;
            background-color: $fill-primary-bg-hi1 !important;
        }
        &:visited {
            text-decoration: none;
            background-color: $fill-primary-bg-hi1 !important;
        }

        .dropdown-toggle {
            padding-right: $sp-5 !important;
            gap: $sp-5;
            outline: none;
        }
    }

    &.offline .dropdown,
    &.offline .nav-link {
        background-color: $fill-neutral-bg-hi1 !important;
        &:active,
        &:visited,
        &:hover,
        &:focus,
        &:hover::before {
            color: $fill-neutral-fg-hi1 !important;
            background-color: $fill-neutral-bg-hi1 !important;
        }
    }

    .dropdown-item {
        color: $fill-primary-fg;
        &:focus,
        &:hover,
        &:active,
        &:active:focus {
            background-color: unset;
            color: $fill-primary-fg-hi1 !important;
        }
    }

    .dropdown-menu {
        margin-top: 0 !important;
        position: relative !important;
        top: 0;
        display: block;
        visibility: hidden;
        &.show {
            visibility: visible;
        }
    }

    .nav-link {
        height: 100% !important;
        display: flex;
        align-items: center;
        padding-left: math.div($avatar-width-sm, 2) + $sp-5 !important;
    }
}
</style>
