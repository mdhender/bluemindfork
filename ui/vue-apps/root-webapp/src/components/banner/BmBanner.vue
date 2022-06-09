<template>
    <bm-navbar
        :aria-label="$t('banner.label')"
        variant="primary"
        class="bm-banner p-0 align-items-center d-none d-lg-flex"
    >
        <bm-button
            id="all-apps-popover"
            variant="inline-on-fill-primary"
            class="px-3"
            :title="$t('banner.reach.all_apps')"
            :aria-label="$t('banner.reach.all_apps')"
        >
            <bm-icon icon="9dots" size="2x" />
        </bm-button>
        <bm-banner-applications :applications="applications" target="all-apps-popover" />
        <bm-navbar-brand href="#" :to="logoLink" :title="$t('banner.main.brand')">
            <img src="images/logo-bluemind.png" alt="" />
        </bm-navbar-brand>
        <bm-banner-shortcuts :applications="defaultApps" />
        <div class="app-title text-center flex-grow-1 font-weight-bold text-uppercase">
            <span v-if="selectedApp">{{ selectedApp.name }}</span>
        </div>
        <bm-extension id="webapp.banner" path="widget" class="d-flex align-items-center" />
        <bm-banner-help v-if="selectedApp && selectedApp.help" :url="selectedApp.help" />
        <bm-banner-menu :user="user" class="ml-4" />
    </bm-navbar>
</template>

<script>
import { BmButton, BmIcon, BmNavbar, BmNavbarBrand } from "@bluemind/styleguide";
import { BmExtension } from "@bluemind/extensions";
import BannerL10N from "../../../l10n/banner/";
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
        BmButton,
        BmExtension,
        BmIcon,
        BmNavbar,
        BmNavbarBrand
    },
    componentI18N: { messages: BannerL10N },
    props: {
        applications: {
            required: true,
            type: Array
        },
        user: {
            required: true,
            type: Object
        }
    },
    computed: {
        selectedApp() {
            return this.applications.find(application => this.$route.path.startsWith(application.path));
        },
        logoLink() {
            return this.selectedApp ? this.selectedApp.path : "/";
        },
        defaultApps() {
            return this.applications.filter(({ shortcut }) => shortcut);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";
@import "~@bluemind/styleguide/css/mixins/_buttons";

.bm-banner {
    min-height: fit-content;
}

.bm-banner .app-title {
    color: $secondary-fg;
}

.bm-banner .fa-9dots {
    cursor: pointer;
}

.bm-banner .navbar-brand {
    margin-right: 0;
    padding: 0;
    display: inline-flex;
    align-items: center;
    text-align: center;
    width: 150px;
    height: 43px;
}

.bm-banner #all-apps-popover:focus,
.bm-banner #all-apps-popover:hover {
    &::before {
        opacity: 0;
    }
}
</style>
