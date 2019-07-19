<template>
    <bm-navbar-nav fill class="bm-banner-applications ">
        <bm-nav-item 
            v-for="app in applications" 
            :key="app.href" 
            :to="app.external ? '' : app.href" 
            :href="app.external ? app.href : ''"
            class="px-2"
        >
            <div class="d-inline-block">
                <span v-if="!!app.icon.svg" v-html="app.icon.svg" />
                <bm-icon v-else-if="!!app.icon.name" :name="app.icon.name" />
                <img v-else-if="!!app.icon.url" :href="app.icon.url" aria-hidden="true">
                <bm-icon v-else name="" />
                <div class="d-inline-block align-middle">{{ app.name }}</div>
            </div>
            <bm-badge>{{ app.count }}</bm-badge>
        </bm-nav-item>
    </bm-navbar-nav>
</template>

<script>
import { BmBadge, BmNavbarNav, BmNavItem } from "@bluemind/styleguide";

export default {
    name: "BmBannerApplications",
    components: {
        BmBadge,
        BmNavbarNav,
        BmNavItem,
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

.bm-banner-applications .router-link-active, .bm-banner-applications .nav-link:hover {
    color: $primary !important;
    border-color: $primary !important;
}

.bm-banner-applications .nav-link.router-link-active {
    color: $primary !important;
    font-weight: $font-weight-bold;
}

.bm-banner-applications .nav-link {
    text-transform: uppercase;
    border-bottom: transparent solid 3px;
    color: color-yiq(theme-color("info-dark")) !important;
    font-size: $font-size-lg;
}
</style>
