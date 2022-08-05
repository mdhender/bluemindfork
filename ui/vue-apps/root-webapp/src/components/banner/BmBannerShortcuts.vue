<template>
    <bm-navbar-nav fill class="bm-banner-shortcuts align-items-end" role="menubar">
        <bm-nav-item
            v-for="app in applications"
            :key="app.$id"
            :to="app.external ? '' : app.path"
            :href="app.external ? app.path : ''"
            class="px-2"
            role="menuitem"
        >
            <bm-app-icon :icon-app="app.icon" />
            <span class="sr-only">{{ app.name }}</span>
            <bm-badge v-if="app.count">{{ app.count }}</bm-badge>
        </bm-nav-item>
    </bm-navbar-nav>
</template>

<script>
import { BmBadge, BmNavbarNav, BmNavItem } from "@bluemind/styleguide";
import BmAppIcon from "../BmAppIcon";

export default {
    name: "BmBannerShortcuts",
    components: {
        BmBadge,
        BmAppIcon,
        BmNavbarNav,
        BmNavItem
    },
    props: {
        applications: {
            required: true,
            type: Array
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

$border-bottom-thickness: 3px;

.bm-banner-shortcuts {
    height: 100%;

    .bm-app-icon svg {
        width: 24px;
        height: 24px;
    }
}
.bm-banner-shortcuts .nav-link {
    padding: $sp-3 0 calc(#{$sp-4} - #{$border-bottom-thickness}) 0;
    width: 48px;
    display: flex;
    justify-content: center;
    border-bottom: transparent solid $border-bottom-thickness;
    color: $fill-primary-fg !important;
}

.bm-banner-shortcuts .router-link-active,
.bm-banner-shortcuts .nav-link:hover {
    color: $fill-primary-fg-hi1 !important;
    border-color: $fill-primary-fg-hi1 !important;
}

.bm-banner-shortcuts .nav-link.router-link-active {
    color: $fill-primary-fg-hi1 !important;
    font-weight: $font-weight-bold;
}
</style>
