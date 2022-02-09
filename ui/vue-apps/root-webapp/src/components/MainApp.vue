<template>
    <div class="main-app d-flex flex-column h-100 bg-light">
        <global-events target="self" @resize="appHeight" />
        <bm-banner v-if="showBanner" :applications="applications" :user="user" />
        <preferences v-if="showPreferences" :applications="applications" />
        <about v-if="showAbout" :version="software.version" />
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
import { mapActions, mapMutations, mapState } from "vuex";
import GlobalEvents from "vue-global-events";

import { mapExtensions } from "@bluemind/extensions";
import CommonL10N from "@bluemind/l10n";
import { inject } from "@bluemind/inject";
import { BmAlertArea } from "@bluemind/styleguide";

import About from "./About";
import BmBanner from "./banner/BmBanner";
import Preferences from "./preferences/Preferences";

const BASE_URI = new RegExp("^" + new URL(document.baseURI).pathname.replace(/\/[^/]*$/, ""));

export default {
    components: {
        About,
        BmBanner,
        Preferences,
        GlobalEvents,
        BmAlertArea
    },
    componentI18N: { messages: CommonL10N },

    data() {
        const session = inject("UserSession");

        return {
            applications: mapExtensions("webapp.banner", { apps: "application" })
                .apps.filter(({ role }) => session.roles.includes(role))
                .map(application => ({
                    ...application,
                    path: BASE_URI.test(application.href) ? application.href.replace(BASE_URI, "") : application.href,
                    external: !BASE_URI.test(application.href)
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
            }
        };
    },
    computed: {
        ...mapState({ applicationAlerts: state => state.alert.applicationAlerts }),
        ...mapState({ alerts: state => state.alert.filter(({ area }) => !area) }),
        ...mapState("root-app", ["appState"]),
        ...mapState("preferences", ["showPreferences"]),
        showAbout() {
            return this.$route.hash && this.$route.hash === "#about";
        },
        showBanner() {
            return !["Thunderbird", "Icedove"].some(agent => new RegExp(agent).test(window.navigator.userAgent));
        }
    },
    created() {
        this.appHeight();
        if (inject("UserSession").userId) {
            this.FETCH_MY_MAILBOX_QUOTA();
            window.setInterval(() => this.FETCH_MY_MAILBOX_QUOTA(), 1000 * 60 * 30);
            this.FETCH_IDENTITIES(this.$store.state.settings.lang);

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
@import "~@bluemind/styleguide/css/_variables";
body {
    height: 100vh; // fallback for the following line (if not supported by browser)
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

@media print {
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
