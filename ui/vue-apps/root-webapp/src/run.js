import Vue from "vue";
import Vue2TouchEvents from "vue2-touch-events";
import VueI18n from "vue-i18n";

import { default as AlertStore, DefaultAlert } from "@bluemind/alert.store";
import { generateDateTimeFormats, InheritTranslationsMixin } from "@bluemind/i18n";
import injector, { inject } from "@bluemind/inject";
import router from "@bluemind/router";
import { initSentry } from "@bluemind/sentry";
import store from "@bluemind/store";
import { BmModalPlugin } from "@bluemind/ui-components";
import VueBus from "@bluemind/vue-bus";
import { extend } from "@bluemind/vuex-router";
import VueSockjsPlugin from "@bluemind/vue-sockjs";

import routes from "./routes";
import registerDependencies from "./registerDependencies";
import PreferencesStore from "./preferences/store";
import RootAppStore from "./rootAppStore";
import SettingsStore from "./settingsStore";
import MainApp from "./components/MainApp";
import NotificationManager from "./NotificationManager";
import Command from "../plugins/Command";

const userSession = window.bmcSessionInfos;
registerDependencies(userSession);
initWebApp(userSession);
initSentry(userSession);

async function initWebApp(userSession) {
    initStore();
    setVuePlugins(userSession);
    Vue.component("DefaultAlert", DefaultAlert);
    const i18n = await initI18N(userSession);
    router.addRoutes(routes);
    new Vue({ el: "#app", i18n, render: h => h(MainApp), router, store });
    if (userSession.userId) {
        new NotificationManager().setNotificationWhenReceivingMail(userSession);
    }
}

function setVuePlugins(userSession) {
    Vue.use(VueI18n);
    Vue.use(VueBus, store);
    if (userSession.userId) {
        Vue.use(VueSockjsPlugin, VueBus);
    }
    Vue.use(Vue2TouchEvents, { disableClick: true });
    Vue.use(BmModalPlugin);
    Vue.use(Command);
}

function initStore() {
    extend(router, store);
    store.registerModule("alert", AlertStore);
    store.registerModule("root-app", RootAppStore);
    store.registerModule("settings", SettingsStore);
    store.registerModule("preferences", PreferencesStore);
}

async function initI18N(userSession) {
    let lang, timeformat;
    if (userSession.userId) {
        const langPromise = inject("UserSettingsPersistence").getOne(userSession.userId, "lang");
        const timeformatPromise = inject("UserSettingsPersistence").getOne(userSession.userId, "timeformat");
        [lang, timeformat] = await Promise.all([langPromise, timeformatPromise]);
    }

    // lang can be any of AvailableLanguages
    Vue.mixin(InheritTranslationsMixin);

    let fallbackLang = "en";
    const navigatorLang = navigator.language;
    if (navigatorLang) {
        fallbackLang = navigator.language.split("-")[0];
    }
    const dateTimeFormats = generateDateTimeFormats(timeformat);

    const i18n = new VueI18n({ locale: lang, fallbackLocale: fallbackLang, dateTimeFormats });
    injector.register({ provide: "i18n", use: i18n });
    return i18n;
}

async function showNotification(message) {
    const result = await Notification.requestPermission();
    if (result === "granted") {
        navigator.serviceWorker.ready.then(function (registration) {
            registration.showNotification("Periodic Sync", {
                body: message
            });
        });
    }
}

(async () => {
    if ("serviceWorker" in navigator) {
        try {
            const registration = await navigator.serviceWorker.register("service-worker.js");
            // eslint-disable-next-line no-console
            console.log("Registration succeeded. Scope is " + registration.scope);
        } catch (error) {
            // eslint-disable-next-line no-console
            console.log("Registration failed with " + error);
        }

        navigator.serviceWorker.addEventListener("message", event => {
            if (event.data.type === "ERROR") {
                showNotification(event.data.payload.message);
            }
        });

        navigator.serviceWorker.addEventListener("waiting", () => {
            // eslint-disable-next-line no-console
            console.warn(
                "A new service worker is installed but cannot be activated until all tabs running the current version have been fully unloaded."
            );
        });

        navigator.serviceWorker.addEventListener("installed", event => {
            if (event.isUpdate) {
                showNotification("A new version of the site is available, please refresh the page.");
            }
        });
    }
})();
