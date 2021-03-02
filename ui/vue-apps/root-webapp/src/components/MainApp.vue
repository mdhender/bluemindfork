<template>
    <div class="main-app d-flex flex-column h-100 bg-light">
        <global-events target="self" @resize="appHeight" />
        <bm-banner :applications="applications" :widgets="widgets" :user="user" :software="software" />
        <preferences v-if="showPreferences" :user="user" :applications="applications" />
        <bm-spinner v-if="appState == 'loading'" :size="2" class="d-flex flex-fill align-self-center" />
        <div
            v-if="appState == 'error'"
            class="text-danger text-center h2 d-flex flex-fill align-self-center align-items-center"
        >
            {{ $t("common.application.bootstrap.error") }}<br />
            {{ $t("common.application.bootstrap.error.solution") }}
        </div>
        <router-view v-else :class="showPreferences ? 'd-none' : 'd-flex'" />
        <bm-alert-area :alerts="alerts" class="z-index-250 position-absolute" @remove="REMOVE">
            <template v-slot="context">
                <component :is="context.alert.renderer" :alert="context.alert" />
            </template>
        </bm-alert-area>
    </div>
</template>

<script>
import { BmSpinner, BmAlertArea } from "@bluemind/styleguide";
import { mapActions, mapMutations, mapState } from "vuex";
import GlobalEvents from "vue-global-events";
import "@bluemind/styleguide/css/bluemind.scss";
import CommonL10N from "@bluemind/l10n";
import injector from "@bluemind/inject";
import BmBanner from "./banner/BmBanner";
import Preferences from "./preferences/Preferences";

export default {
    components: {
        BmBanner,
        BmSpinner,
        Preferences,
        GlobalEvents,
        BmAlertArea
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
        return data;
    },
    computed: {
        ...mapState({ applicationAlerts: state => state.alert.applicationAlerts }),
        ...mapState({ alerts: "alert" }),
        ...mapState("root-app", ["appState"]),
        ...mapState("preferences", ["showPreferences"])
    },
    async created() {
        this.appHeight();
        this.FETCH_MY_MAILBOX_QUOTA();
        window.setInterval(() => this.FETCH_MY_MAILBOX_QUOTA(), 1000 * 60 * 30);

        if (this.$route.hash && this.$route.hash.startsWith("#preferences-")) {
            this.TOGGLE_PREFERENCES();
        }
    },
    methods: {
        ...mapActions("root-app", ["FETCH_MY_MAILBOX_QUOTA"]),
        ...mapActions("alert", ["REMOVE"]),
        ...mapMutations("preferences", ["TOGGLE_PREFERENCES"]),
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
    overflow: hidden;
}

.main-app .bm-application-alert,
.main-app .bm-alert-area {
    bottom: $sp-5;
    left: $sp-2;
    right: $sp-2;
}
@include media-breakpoint-up(lg) {
    .main-app .bm-application-alert,
    .main-app .bm-alert-area {
        bottom: $sp-4;
        padding-left: $sp-4;
        padding-right: $sp-4;
        left: initial;
        right: initial;
    }
}
</style>
