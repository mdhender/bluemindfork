<template>
    <div class="main-app d-flex flex-column h-100 bg-light">
        <global-events target="self" @resize="appHeight" />
        <bm-banner :applications="applications" :widgets="widgets" :user="user" :software="software" />
        <preferences v-if="showPreferences" :user="user" :applications="applications" />
        <div
            v-if="appState == 'error'"
            class="text-danger text-center h2 d-flex flex-fill align-self-center align-items-center"
        >
            {{ $t("common.application.bootstrap.error") }}<br />
            {{ $t("common.application.bootstrap.error.solution") }}
        </div>
        <router-view v-else :class="showPreferences ? 'd-none' : 'd-flex'" />
        <bm-alert-area :alerts="alerts" :floating="true" @remove="REMOVE">
            <template v-slot="context">
                <component :is="context.alert.renderer" :alert="context.alert" />
            </template>
        </bm-alert-area>
    </div>
</template>

<script>
import { BmAlertArea } from "@bluemind/styleguide";
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
        Preferences,
        GlobalEvents,
        BmAlertArea
    },
    componentI18N: { messages: CommonL10N },

    data() {
        const data = {};
        data.applications = [];
        data.widgets = [];
        const baseURI = new RegExp("^" + new URL(document.baseURI).pathname.replace(/\/[^/]*$/, ""));
        window.bmExtensions_["net.bluemind.banner"].map(function (extension) {
            if (extension.application) {
                const entry = extension.application;
                data.applications.push({
                    id: extension.bundle,
                    icon: {
                        name: entry.children["icon-name"] && entry.children["icon-name"].body,
                        svg: entry.children["icon-svg"] && entry.children["icon-svg"].body,
                        url: entry.children["icon-url"] && entry.children["icon-url"].body
                    },
                    fullPath: entry.href,
                    href: baseURI.test(entry.href) ? entry.href.replace(baseURI, "") : entry.href,
                    external: !baseURI.test(entry.href),
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

        const user = userSession.userId
            ? {
                  displayname: userSession["formatedName"],
                  email: userSession["defaultEmail"]
              }
            : {
                  displayname: "Anonymous",
                  email: "anonymous@noreply.local"
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
        ...mapState({ alerts: state => state.alert.filter(({ area }) => !area) }),
        ...mapState("root-app", ["appState"]),
        ...mapState("preferences", ["showPreferences"]),
        ...mapState("session", { settings: ({ settings }) => settings.remote })
    },
    created() {
        this.appHeight();
        if (injector.getProvider("UserSession").get().userId) {
            this.FETCH_MY_MAILBOX_QUOTA();
            window.setInterval(() => this.FETCH_MY_MAILBOX_QUOTA(), 1000 * 60 * 30);
            this.FETCH_IDENTITIES(this.settings.lang);

            this.$router.onReady(() => {
                if (this.$route.hash && this.$route.hash.startsWith("#preferences-")) {
                    this.TOGGLE_PREFERENCES();
                }
            });
        }
    },
    methods: {
        ...mapActions("root-app", ["FETCH_IDENTITIES", "FETCH_MY_MAILBOX_QUOTA"]),
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

.main-app > .bm-alert-area {
    bottom: $sp-5;
    left: $sp-2;
    right: $sp-2;
}
@include media-breakpoint-up(lg) {
    .main-app > .bm-alert-area {
        bottom: $sp-4;
        padding-left: $sp-4;
        padding-right: $sp-4;
        left: initial;
        right: initial;
    }
}
</style>
