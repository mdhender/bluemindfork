<template>
    <bm-navbar type="dark" variant="info-dark" class="bm-banner p-0 align-items-stretch d-none d-lg-flex">
        <bm-button
            id="all-apps-popover"
            v-bm-tooltip
            variant="link"
            class="px-3 align-self-center"
            :title="$t('banner.reach.all_apps')"
            :aria-label="$t('banner.reach.all_apps')"
        >
            <bm-icon icon="9dots" size="2x" />
        </bm-button>
        <bm-popover
            ref="apps-popover"
            target="all-apps-popover"
            placement="bottomright"
            custom-class="apps-popover"
            triggers="click blur"
            variant="info-dark"
            @shown="setFocus"
        >
            <div class="text-white mb-2 mx-3">{{ $t("banner.main.apps") }}</div>
            <bm-row class="bm-apps">
                <bm-col v-for="app in applications" :key="app.href" cols="6" class="text-white">
                    <a v-if="app.external" :href="app.href">
                        <div class="pl-3 my-2 bm-app">
                            <bm-banner-app-icon :icon-app="app.icon" />
                            <span class="pl-2 text-uppercase align-middle">{{ app.name }}</span>
                        </div>
                    </a>
                    <router-link v-else :to="app.href" tag="div" class="pl-3 my-2 bm-app" @click.native="closePopover">
                        <bm-banner-app-icon :icon-app="app.icon" />
                        <span class="pl-2 text-uppercase align-middle">{{ app.name }}</span>
                    </router-link>
                </bm-col>
            </bm-row>
        </bm-popover>
        <bm-navbar-brand v-bm-tooltip href="#" :to="logoLink" :title="$t('banner.main.brand')">
            <!-- eslint-disable-next-line vue/no-v-html -->
            <svg height="30" width="120" v-html="logo" />
        </bm-navbar-brand>
        <bm-banner-applications :applications="defaultApps" />
        <div class="app-title text-center flex-grow-1 align-self-center font-weight-bold text-uppercase">
            <span v-if="selectedApp">{{ selectedApp.name }}</span>
        </div>
        <component :is="widget.component" v-for="widget in widgets" :key="widget.component" />
        <bm-banner-user :user="user" @openPreferences="$emit('openPreferences')" />
    </bm-navbar>
</template>

<script>
import BannerL10N from "../../l10n/banner/";
import BmBannerApplications from "./BmBannerApplications";
import BmBannerAppIcon from "./BmBannerAppIcon";
import BmBannerUser from "./BmBannerUser";
import {
    BmButton,
    BmCol,
    BmIcon,
    BmLogo,
    BmNavbar,
    BmNavbarBrand,
    BmPopover,
    BmRow,
    BmTooltip
} from "@bluemind/styleguide";

export default {
    name: "BmBanner",
    components: {
        BmBannerApplications,
        BmBannerAppIcon,
        BmBannerUser,
        BmButton,
        BmCol,
        BmIcon,
        BmNavbar,
        BmNavbarBrand,
        BmPopover,
        BmRow
    },
    directives: { BmTooltip },
    componentI18N: { messages: BannerL10N },
    props: {
        applications: {
            required: true,
            type: Array
        },
        widgets: {
            required: true,
            type: Array
        },
        software: {
            type: Object,
            required: true
        },
        user: {
            required: true,
            type: Object
        }
    },
    data() {
        return {
            logo: BmLogo,
            selectedAppPath: ""
        };
    },
    computed: {
        selectedApp() {
            return this.applications.find(application => this.$route.path.startsWith(application.href));
        },
        logoLink() {
            return this.selectedApp ? this.selectedApp.href : "/";
        },
        defaultApps() {
            return this.applications.filter(
                app => app.href === "/mail/" || app.href === "/contact/" || app.href === "/cal/"
            );
        }
    },
    methods: {
        closePopover() {
            this.$root.$emit("bv::hide::popover", "all-apps-popover");
        },
        setFocus() {
            this.$nextTick(() => document.getElementsByClassName("apps-popover")[0].focus());
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.bm-banner .app-title {
    color: $primary;
}

.bm-banner .fa-9dots,
.apps-popover .bm-app {
    cursor: pointer;
}

.bm-banner .fa-9dots {
    color: color-yiq(theme-color("info-dark")) !important;
}

.bm-banner .navbar-brand {
    margin-right: 0;
}

.bm-banner #all-apps-popover:focus {
    outline: 1px dotted $white;
}

.bm-banner #all-apps-popover:focus,
.bm-banner #all-apps-popover:hover {
    background-color: unset;
    &::before {
        opacity: 0;
    }
}

.apps-popover {
    max-width: unset !important;
    left: -23px !important;
}

.apps-popover .arrow {
    left: 17px !important;
}

.apps-popover .bm-apps {
    width: 22rem;
    a:focus {
        outline: 1px dotted $light;
    }
}

.apps-popover .bm-app:hover,
.apps-popover a:focus,
.apps-popover .bm-app.router-link-active {
    color: $primary;
}

.apps-popover .bm-app.router-link-active {
    font-weight: $font-weight-bold;
}

.apps-popover a,
.apps-popover a:hover {
    all: unset;
}

.apps-popover .popover-body {
    padding-right: 0;
    padding-left: 0;
}
</style>
