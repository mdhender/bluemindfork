import Vue from "vue";
import Vue2TouchEvents from "vue2-touch-events";
import VueI18n from "vue-i18n";

import { default as AlertStore, DefaultAlert } from "@bluemind/alert.store";
import { DateTimeFormats, FirstDayOfWeek, InheritTranslationsMixin } from "@bluemind/i18n";
import injector from "@bluemind/inject";
import router from "@bluemind/router";
import store from "@bluemind/store";
import { BmModalPlugin } from "@bluemind/styleguide";
import { UserSettingsClient } from "@bluemind/user.api";
import VueBus from "@bluemind/vue-bus";
import { extend } from "@bluemind/vuex-router";
import VueSockjsPlugin from "@bluemind/vue-sockjs";

import RootAppStore from "./rootAppStore";
import SessionStore from "./sessionStore";
import MainApp from "./components/MainApp";
import NotificationManager from "./NotificationManager";

initWebApp();

function initWebApp() {
    registerUserSession();
    const userSession = injector.getProvider("UserSession").get();
    registerDependencies(userSession);
    initStore();
    setVuePlugins();
    const i18n = initI18N(userSession);
    Vue.component("DefaultAlert", DefaultAlert);
    new Vue({
        el: "#app",
        i18n,
        render: h => h(MainApp),
        router,
        store
    });

    new NotificationManager().setNotificationWhenReceivingMail(userSession);
}

function setVuePlugins() {
    Vue.use(VueI18n);
    Vue.use(VueBus, store);
    Vue.use(VueSockjsPlugin, VueBus);
    Vue.use(Vue2TouchEvents, { disableClick: true });
    Vue.use(BmModalPlugin);
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
    store.registerModule("session", SessionStore);
}

function registerDependencies(userSession) {
    // if no lang defined, use monday as fdow
    let firstDayOfWeek = FirstDayOfWeek[userSession.lang.toUpperCase()];
    firstDayOfWeek = firstDayOfWeek >= 0 ? firstDayOfWeek : 1;

    injector.register({
        provide: "Environment",
        use: { firstDayOfWeek }
    });

    injector.register({
        provide: "GlobalEventBus",
        use: VueBus.Client
    });

    injector.register({
        provide: "UserSettingsPersistence",
        factory: () => {
            return new UserSettingsClient(userSession.sid, userSession.domain);
        }
    });
}

function initI18N(userSession) {
    Vue.mixin(InheritTranslationsMixin);
    const i18n = new VueI18n({ locale: userSession.lang, fallbackLocale: "en", dateTimeFormats: getDateTimeFormats() });

    injector.register({
        provide: "i18n",
        use: i18n
    });

    return i18n;
}

function getDateTimeFormats() {
    return {
        fr: DateTimeFormats,
        en: DateTimeFormats
    };
}
