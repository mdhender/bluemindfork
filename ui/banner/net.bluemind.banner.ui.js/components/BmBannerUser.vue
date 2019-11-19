<template>
    <bm-navbar-nav class="bm-banner-user order-0 order-lg-1 align-items-center">
        <bm-nav-item-dropdown right offset="5">
            <template slot="button-content">
                <bm-avatar :alt="user.displayname" class="flex-shrink-0" />
                <span v-bm-tooltip.hover.ds500 class="pr-4 username text-truncate m-auto" :title="user.displayname">
                    {{ user.displayname }}
                </span>
            </template>
            <bm-dropdown-item 
                v-bm-tooltip.left.hover.d500="$t('banner.menu.settings.aria')"
                href="/settings/"
                :aria-label="$t('banner.menu.settings.aria')"
            >
                <bm-label-icon icon="preferences">{{ $t("banner.menu.settings") }} </bm-label-icon>
            </bm-dropdown-item>
            <bm-dropdown-item href="#">
                <bm-label-icon icon="sign-out">{{ $t("banner.menu.logout") }} </bm-label-icon>
            </bm-dropdown-item>
        </bm-nav-item-dropdown>
    </bm-navbar-nav>
</template>

<script>
import { BmAvatar, BmDropdownItem, BmLabelIcon, BmNavbarNav, BmNavItemDropdown, BmTooltip} from "@bluemind/styleguide";

export default {
    name: "BmBannerUser",
    components: {
        BmAvatar,
        BmDropdownItem,
        BmLabelIcon,
        BmNavbarNav,
        BmNavItemDropdown
    },
    directives: {BmTooltip},
    props: {
        user: {
            required: true,
            type: Object
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

$contrasted-color : color-yiq(theme-color("info-dark")) !important;

.bm-banner-user .bm-avatar {
    position: relative;
    left: -1em;
}

.bm-banner-user .username {
    color: $contrasted-color;
}

.bm-banner-user, .bm-banner-user .dropdown-menu {
    background-color: theme-color-level("info-dark", 4) !important;
}

.bm-banner-user .dropdown-menu {
    color: $contrasted-color;
    margin-top: 0 !important;
}

.bm-banner-user .dropdown-toggle, .bm-banner-user .dropdown-menu {
    min-width: 10rem;
    max-width: 15rem;
}

.bm-banner-user .dropdown-toggle {
    display: flex;
    align-items: center;
    padding-left: 0!important;
    text-decoration: none;
    &:active, &:visited, &:hover, &:focus {
        text-decoration: none;
    }
    &:after {
        position: absolute;
        right: map-get($spacers, 3);
        border-top-color: $contrasted-color;
        border-bottom-color: $contrasted-color;
        vertical-align: middle;
    }
}

.bm-banner-user .dropdown-item {
    padding: map-get($spacers, 2) map-get($spacers, 3);
    color: $contrasted-color;
    &:focus, &:hover, &:active, &:active:focus {
        background-color: unset;
        color: $primary !important;
    }
}
</style>
