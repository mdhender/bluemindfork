<template>
    <bm-navbar type="dark" variant="info-dark" toggleable="lg" class="bm-banner p-0 align-items-stretch">
        <div id="all-apps-popover" class="px-3 align-self-center">
            <bm-icon icon="9dots" size="2x" />
        </div>
        <bm-popover
            ref="apps-popover"
            target="all-apps-popover"
            placement="bottomright"
            custom-class="apps-popover"
            triggers="click blur"
            variant="info-dark"
            @shown="setFocus"
        >
            <div class="text-white mt-1 mb-2 ml-2">{{ $t("banner.main.apps") }} </div>
            <bm-row class="bm-apps">
                <bm-col v-for="app in applications" :key="app.href" cols="6" class="text-white">
                    <a v-if="app.external" :href="app.href">
                        <div class="pl-3 my-2 bm-app">
                            <bm-banner-app-icon :icon-app="app.icon" />
                            <span class="pl-2 text-uppercase">{{ app.name }}</span>
                        </div>
                    </a>
                    <router-link v-else :to="app.href" tag="div" class="pl-3 my-2 bm-app" @click.native="closePopover">
                        <bm-banner-app-icon :icon-app="app.icon" />
                        <span class="pl-2 text-uppercase">{{ app.name }}</span>
                    </router-link>
                </bm-col>
            </bm-row>
        </bm-popover>
        <bm-navbar-brand :title="$t('banner.main.brand', software)" href="#" :to="logoLink">
            <svg height="30" width="120" v-html="logo" />
        </bm-navbar-brand>
        <bm-banner-applications :applications="defaultApps" />
        <div class="app-title text-center flex-grow-1 align-self-center font-weight-bold text-uppercase">
            <span v-if="selectedApp">{{ selectedApp.name }}</span>
        </div>
        <component :is="widget.component" v-for="widget in widgets" :key="widget.component" />
        <bm-banner-user :user="user" />
    </bm-navbar>
</template>

<script>
import BannerL10N from "@bluemind/banner.l10n";
import BmBannerApplications from "./BmBannerApplications";
import BmBannerAppIcon from "./BmBannerAppIcon";
import BmBannerUser from "./BmBannerUser";
import { BmCol, BmIcon, BmLogo, BmNavbar, BmNavbarBrand, BmPopover, BmRow } from "@bluemind/styleguide";

export default {
    name: "BmBanner",
    components: {
        BmBannerApplications,
        BmBannerAppIcon,
        BmBannerUser,
        BmCol,
        BmIcon,
        BmNavbar,
        BmNavbarBrand,
        BmPopover,
        BmRow
    },
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
            return this.applications.filter(app => 
                app.href === "/mail/" || app.href === "/contact/" || app.href === "/cal/"
            );
        }
    },
    methods: {
        closePopover() {
            this.$root.$emit('bv::hide::popover', 'all-apps-popover');
        },
        setFocus() {
            this.$nextTick(() => document.getElementsByClassName("apps-popover")[0].focus());
        }
    }
};
</script>

<style lang="scss">
@import '~@bluemind/styleguide/css/_variables';

.bm-banner .app-title {
    color: $primary;
}

.bm-banner .fa-9dots, .apps-popover .bm-app {
    cursor: pointer;
}

.bm-banner .fa-9dots {
    color: color-yiq(theme-color("info-dark")) !important;   
}

.bm-banner .navbar-brand {
    margin-right: 0;
}

.apps-popover {
    max-width: unset !important;
    margin-top: map-get($spacers, 3) !important;
    left: -5px !important;
}

.apps-popover .arrow {
    left: 17px !important;
}

.apps-popover .bm-apps {
    width: 22rem;
}

.apps-popover .bm-app {
    border-left: transparent solid 3px;
}

.apps-popover .bm-app:hover, .apps-popover .bm-app.router-link-exact-active {
    color: $primary;
    border-left: $primary solid 3px;
}

.apps-popover a, .apps-popover a:hover {
    all: unset;
}

.apps-popover .popover-body {
    padding-right: 0;
    padding-left: 0;
}
</style>