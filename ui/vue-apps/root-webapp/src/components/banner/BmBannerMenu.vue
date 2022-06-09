<template>
    <bm-navbar-nav
        class="bm-banner-user order-0 order-lg-1 align-self-stretch align-items-stretch"
        :class="{ offline: !isOnline }"
    >
        <bm-nav-item v-if="!logged" :href="loginUrl" class="flex-fill">
            <bm-avatar :alt="user.displayname" class="flex-shrink-0" width="2em" :status="status" />
            <span class="username text-truncate w-100 text-center">
                {{ $t("banner.login") }}
            </span>
        </bm-nav-item>
        <bm-nav-item-dropdown right offset="5" class="flex-fill">
            <template v-if="logged" slot="button-content">
                <bm-avatar :alt="user.displayname" :urn="user.urn" class="flex-shrink-0" width="2em" :status="status" />
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
            <bm-dropdown-item v-if="logged" icon="sign-out" href="/bluemind_sso_logout">
                {{ $t("banner.menu.logout") }}
            </bm-dropdown-item>
            <bm-dropdown-item v-else icon="user" :href="loginUrl">
                {{ $t("banner.login") }}
            </bm-dropdown-item>
        </bm-nav-item-dropdown>
    </bm-navbar-nav>
</template>

<script>
import { inject } from "@bluemind/inject";
import BmRoles from "@bluemind/roles";
import { BmAvatar, BmDropdownItem, BmNavbarNav, BmNavItem, BmNavItemDropdown } from "@bluemind/styleguide";

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
            return `/login/index.html?askedUri=${this.$router.options.base}${this.$route.fullPath}`;
        }
    },
    methods: {
        ...mapMutations("preferences", ["TOGGLE_PREFERENCES"])
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.bm-banner-user {
    min-width: 10rem;
    max-width: 15rem;
    .bm-avatar {
        position: relative;
        left: -1em;
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
    }

    .nav-link {
        height: 100% !important;
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
