<template>
    <bm-navbar-nav class="bm-banner-user order-0 order-lg-1 align-items-center ">
        <bm-nav-item-dropdown right offset="5">
            <template slot="button-content">
                <bm-avatar :alt="user.displayname" />
                <span v-bm-tooltip.hover.d500 class="pr-3" :title="user.displayname">{{ user.displayname }}</span>
            </template>
            <bm-dropdown-item href="#">
                <bm-label-icon icon="preferences">{{ $t("banner.menu.settings") }} </bm-label-icon>
            </bm-dropdown-item>
            <div class="dropdown-divider" />
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

.bm-banner-user {
    background-color: theme-color-level("info-dark", 4);
}

.bm-banner-user .bm-avatar {
    position: relative;
    left: -1em;
}

.bm-banner-user .nav-link  {
    display: flex;
    align-items: center;
    padding-left: 0!important;
    text-decoration: none;
    &:hover, &:focus, &:active{
        text-decoration: none;
    }
}

.bm-banner-user .nav-link span {
    color: color-yiq(theme-color("info-dark")) !important;
    max-width: 16em;
    text-overflow: ellipsis;
    display: inline-block;
    overflow: hidden;
}

.bm-banner-user .dropdown-toggle::after {
    border-top-color: color-yiq(theme-color("info-dark")) !important;
    border-bottom-color: color-yiq(theme-color("info-dark")) !important;
    vertical-align: middle;
}
</style>
