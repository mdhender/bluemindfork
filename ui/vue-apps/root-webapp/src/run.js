import Vue from "vue";
import Vue2TouchEvents from "vue2-touch-events";
import VueI18n from "vue-i18n";

import { default as AlertStore, DefaultAlert } from "@bluemind/alert.store";
import { generateDateTimeFormats, InheritTranslationsMixin } from "@bluemind/i18n";
import injector from "@bluemind/inject";
import router from "@bluemind/router";
import { initSentry } from "@bluemind/sentry";
import store from "@bluemind/store";
import { BmModalPlugin } from "@bluemind/styleguide";
import UUIDGenerator from "@bluemind/uuid";
import VueBus from "@bluemind/vue-bus";
import { extend } from "@bluemind/vuex-router";
import VueSockjsPlugin from "@bluemind/vue-sockjs";

import registerDependencies from "./registerDependencies";
import PreferencesStore from "./preferences/store";
import RootAppStore from "./rootAppStore";
import SettingsStore from "./settingsStore";
import MainApp from "./components/MainApp";
import NotificationManager from "./NotificationManager";
import Command from "../plugins/Command";

initWebApp();
initSentry(Vue);

async function initWebApp() {
    registerUserSession();
    const userSession = injector.getProvider("UserSession").get();
    registerDependencies(userSession);
    initStore();
    setVuePlugins(userSession);
    if (userSession.userId) {
        await store.dispatch("settings/FETCH_ALL_SETTINGS"); // needed to initialize i18n
        await store.dispatch("root-app/FETCH_ALL_APP_DATA");
    }
    const i18n = initI18N(userSession);
    Vue.component("DefaultAlert", DefaultAlert);
    adaptLegacyNotificationSystem();
    new Vue({
        el: "#app",
        i18n,
        render: h => h(MainApp),
        router,
        store
    });
    if (userSession.userId) {
        new NotificationManager().setNotificationWhenReceivingMail(userSession);
    }
}

function adaptLegacyNotificationSystem() {
    Vue.component("DisplayLegacyNotif", {
        name: "DisplayLegacyNotif",
        props: {
            alert: {
                type: Object,
                default: () => ({})
            }
        },
        template: "<span>{{alert.payload}}</span>"
    });
    document.addEventListener("ui-notification", displayLegacyNotification);
}

function displayLegacyNotification(event) {
    const type = event.detail.type !== "error" ? "SUCCESS" : "ERROR";
    store.dispatch("alert/" + type, {
        alert: { uid: UUIDGenerator.generate(), name: "legacy notif", payload: event.detail.message },
        options: { renderer: "DisplayLegacyNotif" }
    });
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

function registerUserSession() {
    injector.register({
        provide: "UserSession",
        use: window.bmcSessionInfos
    });
}

function initStore() {
    extend(router, store);
    store.registerModule("alert", AlertStore);
    store.registerModule("root-app", RootAppStore);
    store.registerModule("settings", SettingsStore);
    store.registerModule("preferences", PreferencesStore);
}

function initI18N() {
    // lang can be any of AvailableLanguages
    const lang = store.state.settings.lang;

    Vue.mixin(InheritTranslationsMixin);

    let fallbackLang = "en";
    const navigatorLang = navigator.language;
    if (navigatorLang) {
        fallbackLang = navigator.language.split("-")[0];
    }
    const i18n = new VueI18n({
        locale: lang,
        fallbackLocale: fallbackLang,
        dateTimeFormats: generateDateTimeFormats(store.state.settings.timeformat)
    });

    injector.register({
        provide: "i18n",
        use: i18n
    });

    return i18n;
}
