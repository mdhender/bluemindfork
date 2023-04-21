<template>
    <div class="main-app d-flex flex-column h-100">
        <bm-extension id="webapp" path="app.header" />
        <global-events target="self" @resize="appHeight" @dragover.prevent />
        <system-alert-area v-if="systemAlerts.length > 0" :system-alerts="systemAlerts" @remove="systemAlerts = []" />
        <bm-banner v-if="showBanner" :applications="applications" :user="user" :current-application="current" />
        <preferences v-if="showPreferences" :applications="applications" />
        <about v-if="showAbout" :version="software.version" />
        <div
            v-if="appState == 'error'"
            class="text-danger text-center h2 d-flex flex-fill align-self-center align-items-center"
        >
            {{ $t("common.application.bootstrap.error") }}<br />
            {{ $t("common.application.bootstrap.error.solution") }}
        </div>
        <router-view v-else :class="showPreferences ? 'd-none d-lg-flex' : 'd-flex'" />
        <bm-alert-area class="main-alert-area" :alerts="alerts" :floating="true" @remove="REMOVE">
            <template v-slot="context">
                <component :is="context.alert.renderer" :alert="context.alert" />
            </template>
        </bm-alert-area>
    </div>
</template>

<script>
import { mapActions, mapMutations, mapState } from "vuex";
import GlobalEvents from "vue-global-events";

import { mapExtensions } from "@bluemind/extensions";
import { BmExtension } from "@bluemind/extensions.vue";
import { inject } from "@bluemind/inject";
import { BmAlertArea } from "@bluemind/ui-components";

import About from "./About";
import BaseUri from "../routes/BaseUriRegExp";
import BmBanner from "./banner/BmBanner";
import Preferences from "./preferences/Preferences";
import SystemAlertArea from "./SystemAlertArea";
import favicon from "../../assets/favicon.png";

export default {
    components: {
        About,
        BmBanner,
        BmExtension,
        Preferences,
        GlobalEvents,
        BmAlertArea,
        SystemAlertArea
    },
    data() {
        const session = inject("UserSession");

        return {
            applications: mapExtensions("net.bluemind.webapp", { apps: "application" })
                .apps //Deprecated end-point
                .concat(mapExtensions("webapp.banner", { apps: "application" }).apps)
                .filter(({ role }) => session.roles.includes(role))
                .map(application => ({
                    ...application,
                    path: BaseUri.test(application.href) ? application.href.replace(BaseUri, "") : application.href,
                    external: !BaseUri.test(application.href)
                })),
            user: session.userId
                ? {
                      displayname: session["formatedName"],
                      email: session["defaultEmail"],
                      urn: session["userId"] + "@addressbook_" + session["domain"]
                  }
                : {
                      displayname: "Anonymous",
                      email: "anonymous@noreply.local"
                  },
            software: {
                version: {
                    technical: session["bmVersion"],
                    brand: session["bmBrandVersion"]
                }
            },
            systemAlerts: []
        };
    },
    computed: {
        ...mapState({ alerts: state => state.alert.filter(({ area }) => !area) }),
        ...mapState("root-app", ["appState", "showBanner"]),
        ...mapState("preferences", ["showPreferences"]),
        showAbout() {
            return this.$route.hash && this.$route.hash === "#about";
        },
        current() {
            const path = this.$route.path + (this.$route.path.endsWith("/") ? "" : "/");
            return this.applications.find(application => path.startsWith(application.path));
        }
    },
    watch: {
        current: {
            immediate: true,
            handler() {
                document.title = `${this.current?.name} ${this.$t("common.product")}`.trim();
            }
        },
        "$store.state.settings.theme": {
            immediate: true,
            handler(value, old) {
                document.body.classList.remove(old);
                document.body.classList.add(value);
            }
        }
    },
    async created() {
        const link = document.createElement("link");
        link.href = favicon;
        link.rel = "icon";
        document.getElementsByTagName("head")[0].appendChild(link);

        this.appHeight();

        if (inject("UserSession").userId) {
            this.FETCH_ALL_SETTINGS(this).then(() => {
                this.FETCH_IDENTITIES(this.$store.state.settings.lang);
            });
            this.FETCH_MY_MAILBOX_QUOTA();
            window.setInterval(() => this.FETCH_MY_MAILBOX_QUOTA(), 1000 * 60 * 30);

            this.$router.onReady(() => {
                if (this.$route.hash?.startsWith("#preferences-")) {
                    this.TOGGLE_PREFERENCES();
                }
            });
        }

        this.systemAlerts = await inject("UserAnnouncementsPersistence").get();
        if (["Thunderbird", "Icedove"].some(agent => new RegExp(agent).test(window.navigator.userAgent))) {
            this.$store.commit("root-app/HIDE_BANNER");
        }
    },
    methods: {
        ...mapActions("alert", ["REMOVE"]),
        ...mapActions("root-app", ["FETCH_IDENTITIES", "FETCH_MY_MAILBOX_QUOTA"]),
        ...mapActions("settings", ["FETCH_ALL_SETTINGS"]),
        ...mapMutations("preferences", ["TOGGLE_PREFERENCES"]),
        appHeight() {
            /*
            Fix for mobile : 100vh is too tall, it doesn't count the mobile toolbar
            This issue must be tested on a real phone since browser device simulator does not disply the toolbar
            */
            document.documentElement.style.setProperty("--app-height", window.innerHeight + "px");
        }
    },
    bus: {
        disconnected() {
            let url = window.location.origin + window.location.pathname.replace(/[^/]*$/, "");
            window.location.assign(url);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";
body {
    height: 100vh; // fallback for the following line (if not supported by browser)
    height: var(--app-height);
    overflow: hidden;
}

.main-app {
    background-color: $backdrop;
}

.main-app > .main-alert-area {
    bottom: $sp-5;
    left: $sp-2;
    right: $sp-2;
}
@include media-breakpoint-up(lg) {
    .main-app > .main-alert-area {
        bottom: $sp-6 + $sp-5;
        padding-left: $sp-6;
        padding-right: $sp-6;
        left: initial;
        right: initial;
    }
}

@media print {
    body {
        background-color: $white;
    }
    .main-app {
        height: auto !important;
        .bm-banner {
            display: none !important;
        }
        .btn-toolbar {
            display: none !important;
        }
        .navbar {
            display: none !important;
        }
    }
}
</style>
