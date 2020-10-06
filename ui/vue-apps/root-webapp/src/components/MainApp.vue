<template>
    <div class="main-app d-flex flex-column h-100 bg-light">
        <global-events target="self" @resize="appHeight" />
        <bm-banner
            :applications="applications"
            :widgets="widgets"
            :user="user"
            :software="software"
            @openPreferences="areSettingsOpened = true"
        />
        <bm-settings
            v-if="areSettingsOpened"
            :user="user"
            :applications="applications"
            @close="areSettingsOpened = false"
        />
        <bm-spinner v-if="appState == 'loading'" :size="2" class="d-flex flex-fill align-self-center" />
        <div
            v-else-if="appState == 'error'"
            class="text-danger text-center h2 d-flex flex-fill align-self-center align-items-center"
        >
            {{ $t("common.application.bootstrap.error") }}<br />
            {{ $t("common.application.bootstrap.error.solution") }}
        </div>
        <router-view v-else :class="areSettingsOpened ? 'd-none' : 'd-flex'" />
        <bm-application-alert :alerts="applicationAlerts" class="z-index-250 position-absolute">
            <template v-slot="slotProps">
                <component :is="slotProps.alert.renderer" :alert="slotProps.alert" />
            </template>
        </bm-application-alert>
    </div>
</template>

<script>
import { BmApplicationAlert, BmSpinner } from "@bluemind/styleguide";
import { mapActions, mapState } from "vuex";
import GlobalEvents from "vue-global-events";
import "@bluemind/styleguide/css/bluemind.scss";
import CommonL10N from "@bluemind/l10n";
import injector from "@bluemind/inject";
import BmBanner from "./banner/BmBanner";
import BmSettings from "./settings/BmSettings";

export default {
    components: {
        BmApplicationAlert,
        BmBanner,
        BmSpinner,
        BmSettings,
        GlobalEvents
    },
    componentI18N: { messages: CommonL10N },

    data() {
        const data = {};
        data.applications = [];
        data.widgets = [];
        window.bmExtensions_["net.bluemind.banner"].map(function (extension) {
            if (extension.application) {
                const entry = extension.application;
                data.applications.push({
                    icon: {
                        name: entry.children["icon-name"] && entry.children["icon-name"].body,
                        svg: entry.children["icon-svg"] && entry.children["icon-svg"].body,
                        url: entry.children["icon-url"] && entry.children["icon-url"].body
                    },
                    href: entry.href.match(/^\/webapp/) ? entry.href.replace("/webapp", "") : entry.href,
                    external: !entry.href.match(/^\/webapp/),
                    name: entry.name,
                    description: entry.description,
                    order: entry.order,
                    role: entry.role
                });
            }
            if (extension.widget) {
                data.widgets.push(extension.widget);
            }
            if (extension.notification) {
                data.notifications.push(extension.notification);
            }
        });

        const userSession = injector.getProvider("UserSession").get();

        data.applications = data.applications
            .filter(app => userSession.roles.includes(app.role))
            .sort((a, b) => b.order - a.order);
        data.widgets.sort((a, b) => b.order - a.order);

        const user = {
            displayname: userSession["formatedName"],
            email: userSession["defaultEmail"]
        };
        const software = {
            version: userSession["bmVersion"],
            brand: userSession["bmBrandVersion"]
        };
        data.user = user;
        data.software = software;
        data.areSettingsOpened = false;
        return data;
    },
    computed: {
        ...mapState({ applicationAlerts: state => state.alert.applicationAlerts }),
        ...mapState("root-app", ["appState"])
    },
    created() {
        this.appHeight();
        // initialize user settings
        this.FETCH_ALL_SETTINGS();
    },
    methods: {
        ...mapActions("session", ["FETCH_ALL_SETTINGS"]),
        appHeight() {
            /*
            Fix for mobile : 100vh is too tall, it doesn't count the mobile toolbar
            This issue must be tested on a real phone since browser device simulator does not disply the toolbar
            */
            document.documentElement.style.setProperty("--app-height", window.innerHeight + "px");
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";
body {
    height: 100vh;
    height: var(--app-height);
}

.main-app .bm-application-alert {
    bottom: $sp-5;
    left: $sp-2;
    right: $sp-2;
}
@include media-breakpoint-up(lg) {
    .main-app .bm-application-alert {
        bottom: $sp-4;
        left: $sp-4;
        right: $sp-4;
    }
}
</style>
