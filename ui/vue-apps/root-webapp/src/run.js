import Vue from "vue";
import Vue2TouchEvents from "vue2-touch-events";

import { default as AlertStore, DefaultAlert } from "@bluemind/alert.store";
import i18n, { generateDateTimeFormats } from "@bluemind/i18n";
import { inject } from "@bluemind/inject";
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

function initWebApp(userSession) {
    setDateTimeFormat(userSession);
    initStore();
    setVuePlugins(userSession);
    Vue.component("DefaultAlert", DefaultAlert);
    router.addRoutes(routes);
    new Vue({ el: "#app", i18n, render: h => h(MainApp), router, store });
    if (userSession.userId) {
        new NotificationManager().setNotificationWhenReceivingMail(userSession);
    }
}

function setVuePlugins(userSession) {
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

function setDateTimeFormat(session) {
    inject("UserSettingsPersistence")
        .getOne(session.userId, "timeformat")
        .then(timeformat => i18n.setDateTimeFormat(generateDateTimeFormats(timeformat)));
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
