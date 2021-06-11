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
            <div class="d-inline-block"><bm-app-icon :icon-app="app.icon" /></div>
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

.bm-banner-shortcuts .router-link-active,
.bm-banner-shortcuts .nav-link:hover {
    color: $primary !important;
    border-color: $primary !important;
}

.bm-banner-shortcuts .nav-link.router-link-active {
    color: $primary !important;
    font-weight: $font-weight-bold;
}

.bm-banner-shortcuts .nav-link {
    border-bottom: transparent solid 3px;
    color: color-yiq(theme-color("info-dark")) !important;
}
</style>
