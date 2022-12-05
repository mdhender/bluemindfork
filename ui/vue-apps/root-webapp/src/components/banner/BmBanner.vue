<template>
    <bm-navbar :aria-label="$t('banner.label')" variant="primary" class="bm-banner align-items-center d-none d-lg-flex">
        <bm-icon-button
            id="all-apps-popover"
            variant="regular-on-fill-primary"
            size="lg"
            icon="9dots"
            :title="$t('banner.reach.all_apps')"
            :aria-label="$t('banner.reach.all_apps')"
        />
        <bm-banner-applications :applications="applications" target="all-apps-popover" />
        <bm-navbar-brand href="#" :to="logoLink" :title="$t('banner.main.brand')">
            <img src="images/logo-bluemind.png" alt="" />
        </bm-navbar-brand>
        <bm-banner-shortcuts :applications="defaultApps" />
        <div class="app-title text-center flex-grow-1 font-weight-bold text-uppercase">
            <span v-if="currentApplication">{{ currentApplication.name }}</span>
        </div>
        <bm-extension id="webapp.banner" path="widget" class="d-flex align-items-center" />
        <bm-banner-help v-if="currentApplication && currentApplication.help" :url="currentApplication.help" />
        <bm-banner-menu :user="user" />
    </bm-navbar>
</template>

<script>
import { BmIconButton, BmNavbar, BmNavbarBrand } from "@bluemind/ui-components";
import { BmExtension } from "@bluemind/extensions.vue";
import BmBannerApplications from "./BmBannerApplications";
import BmBannerMenu from "./BmBannerMenu";
import BmBannerHelp from "./BmBannerHelp";
import BmBannerShortcuts from "./BmBannerShortcuts";

export default {
    name: "BmBanner",
    components: {
        BmBannerApplications,
        BmBannerHelp,
        BmBannerMenu,
        BmBannerShortcuts,
        BmExtension,
        BmIconButton,
        BmNavbar,
        BmNavbarBrand
    },
    props: {
        applications: {
            required: true,
            type: Array
        },
        user: {
            required: true,
            type: Object
        },
        currentApplication: {
            type: Object,
            default: null
        }
    },
    computed: {
        logoLink() {
            return this.currentApplication ? this.currentApplication.path : "/";
        },
        defaultApps() {
            return this.applications.filter(({ shortcut }) => shortcut);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.bm-banner {
    &.navbar {
        flex: 0 0 42px;

        padding: 0;
        padding-left: $sp-4;
    }

    #all-apps-popover:focus,
    #all-apps-popover:hover {
        &::before {
            opacity: 0;
        }
    }

    .fa-9dots {
        cursor: pointer;
        margin-right: $sp-3;
    }

    .navbar-brand {
        margin-right: $sp-5;
        padding: 0;
        display: inline-flex;
        align-items: center;
        text-align: center;

        img {
            width: 130px;
        }
    }

    .bm-banner-user {
        margin-left: $sp-6;
    }

    .app-title {
        color: $fill-primary-fg-lo1;
    }
}
</style>
