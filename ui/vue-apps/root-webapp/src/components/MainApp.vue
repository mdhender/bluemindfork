<template>
    <div class="main-app d-flex flex-column h-100">
        <bm-extension id="webapp" path="app.header" />
        <global-events target="self" @dragover.prevent />
        <bm-alert-area
            v-if="systemAlerts.length > 0"
            class="system-alert-area p-0 m-0 z-index-750"
            :alerts="systemAlerts"
            @remove="REMOVE"
        >
            <template #default="context">
                <component :is="context.alert.renderer" :alert="context.alert" />
            </template>
        </bm-alert-area>
        <bm-banner v-if="showBanner" :applications="applications" :user="user" :current-application="current" />
        <preferences v-if="showPreferences" :applications="applications" />
        <about v-if="showAbout" :version="software.version" />
        <anonymous-screen v-if="!isLogged && !isAnonymous" @continue="isAnonymous = true" @login="goToLoginForm()" />
        <app-error v-else-if="appState === 'error'" />
        <router-view v-else :class="showPreferences ? 'd-none d-lg-flex' : 'd-flex'" />
        <bm-alert-area class="main-alert-area" :alerts="alerts" :floating="true" @remove="REMOVE">
            <template #default="context">
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
import AnonymousScreen from "./AnonymousScreen";
import AppError from "./AppError";
import BaseUri from "../routes/BaseUriRegExp";
import BmBanner from "./banner/BmBanner";
import Preferences from "./preferences/Preferences";
import favicon from "../../assets/favicon.png";
import { AlertTypes } from "@bluemind/alert.store";
import { INFO, REMOVE, ERROR, WARNING } from "@bluemind/alert.store";
import { useTimezoneChecker } from "../composables/useTimezoneChecker";
import { useAnnouncementsPersistence } from "../composables/useAnnouncementsPersistence";

export default {
    components: {
        About,
        AnonymousScreen,
        AppError,
        BmBanner,
        BmExtension,
        Preferences,
        GlobalEvents,
        BmAlertArea
    },
    setup() {
        const { checkTimezone } = useTimezoneChecker();
        const { annoucementsPersistence } = useAnnouncementsPersistence();
        return { checkTimezone, annoucementsPersistence };
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
            isAnonymous: false,
            checkTimezoneIntervalId: null
        };
    },
    computed: {
        ...mapState({ alerts: state => state.alert.filter(({ area }) => !area) }),
        ...mapState({ systemAlerts: state => state.alert.filter(({ area }) => area == "system-alert") }),
        ...mapState("root-app", ["appState", "showBanner"]),
        ...mapState("preferences", ["showPreferences"]),
        showAbout() {
            return this.$route.hash && this.$route.hash === "#about";
        },
        current() {
            const path = this.$route.path + (this.$route.path.endsWith("/") ? "" : "/");
            return this.applications.find(application => path.startsWith(application.path));
        },
        isLogged() {
            return Boolean(inject("UserSession").userId);
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
                if (value !== undefined) {
                    document.body.classList.add(value);
                }
            }
        }
    },
    async created() {
        const link = document.createElement("link");
        link.href = favicon;
        link.rel = "icon";
        document.getElementsByTagName("head")[0].appendChild(link);

        if (inject("UserSession").userId) {
            this.FETCH_ALL_SETTINGS(this).then(() => {
                this.FETCH_IDENTITIES(this.$store.state.settings.lang);
                this.checkTimezone();
            });
            this.FETCH_MY_MAILBOX_QUOTA();
            window.setInterval(() => this.FETCH_MY_MAILBOX_QUOTA(), 1000 * 60 * 30);

            this.$router.onReady(() => {
                if (this.$route.hash?.startsWith("#preferences-")) {
                    this.TOGGLE_PREFERENCES();
                }
            });
        }

        this.annoucementsPersistence();

        if (["Thunderbird", "Icedove"].some(agent => new RegExp(agent).test(window.navigator.userAgent))) {
            this.$store.commit("root-app/HIDE_BANNER");
        }
    },
    methods: {
        ...mapActions("alert", { REMOVE, INFO, ERROR, WARNING }),
        ...mapActions("root-app", ["FETCH_IDENTITIES", "FETCH_MY_MAILBOX_QUOTA"]),
        ...mapActions("settings", ["FETCH_ALL_SETTINGS"]),
        ...mapMutations("preferences", ["TOGGLE_PREFERENCES"]),
        goToLoginForm() {
            window.location.href = `?askedUri=${this.$router.options.base}${this.$route.fullPath}`;
        }
    },
    bus: {
        disconnected() {
            let url = window.location.origin + window.location.pathname.replace(/[^/]*$/, "");
            window.location.assign(url);
        }
    }
};

function getAlertType(kind) {
    switch (kind) {
        case "Error":
            return AlertTypes.ERROR;
        case "Info":
            return AlertTypes.INFO;
        case "Warn":
            return AlertTypes.WARNING;
    }
}
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
body {
    height: 100vh; // fallback for the following line (if not supported by browser)
    height: 100dvh;
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

.system-alert-area {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
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
