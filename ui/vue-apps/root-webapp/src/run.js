import Vue from "vue";
import Vue2TouchEvents from "vue2-touch-events";
import VueI18n from "vue-i18n";

import { default as AlertStore, DefaultAlert } from "@bluemind/alert.store";
import { CalendarClient, CalendarsMgmtClient, VEventClient } from "@bluemind/calendar.api";
import { ContainersClient, ContainerManagementClient, OwnerSubscriptionsClient } from "@bluemind/core.container.api";
import { FirstDayOfWeek, generateDateTimeFormats, InheritTranslationsMixin } from "@bluemind/i18n";
import injector from "@bluemind/inject";
import { MailboxesClient } from "@bluemind/mailbox.api";
import router from "@bluemind/router";
import { initSentry } from "@bluemind/sentry";
import store from "@bluemind/store";
import { BmModalPlugin } from "@bluemind/styleguide";
import { UserClient, UserMailIdentitiesClient, UserSettingsClient, UserSubscriptionClient } from "@bluemind/user.api";
import VueBus from "@bluemind/vue-bus";
import { extend } from "@bluemind/vuex-router";
import VueSockjsPlugin from "@bluemind/vue-sockjs";

import PreferencesStore from "./preferencesStore";
import RootAppStore from "./rootAppStore";
import SessionStore from "./sessionStore";
import MainApp from "./components/MainApp";
import NotificationManager from "./NotificationManager";

initWebApp();
initSentry(Vue);

async function initWebApp() {
    registerUserSession();
    const userSession = injector.getProvider("UserSession").get();
    registerDependencies(userSession);
    initStore();
    setVuePlugins(userSession);
    if (userSession.userId) {
        await store.dispatch("session/FETCH_ALL_SETTINGS"); // needed to initialize i18n
        await store.dispatch("root-app/FETCH_IDENTITIES", store.state.session.settings.remote.lang);
    }
    const i18n = initI18N(userSession);
    Vue.component("DefaultAlert", DefaultAlert);
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

function setVuePlugins(userSession) {
    Vue.use(VueI18n);
    Vue.use(VueBus, store);
    if (userSession.userId) {
        Vue.use(VueSockjsPlugin, VueBus);
    }
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
    store.registerModule("preferences", PreferencesStore);
}

function registerDependencies(userSession) {
    // if no lang defined, use monday as fdow
    let firstDayOfWeek = FirstDayOfWeek[userSession.lang.toUpperCase()];
    firstDayOfWeek = firstDayOfWeek >= 0 ? firstDayOfWeek : 1;

    injector.register({
        provide: "ContainersPersistence",
        factory: () => new ContainersClient(userSession.sid)
    });

    injector.register({
        provide: "ContainerManagementPersistence",
        factory: containerUid => new ContainerManagementClient(userSession.sid, containerUid)
    });

    injector.register({
        provide: "CalendarsMgmtPersistence",
        factory: () => new CalendarsMgmtClient(userSession.sid)
    });

    injector.register({
        provide: "CalendarPersistence",
        factory: containerUid => new CalendarClient(userSession.sid, containerUid)
    });

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
        factory: () => new UserSettingsClient(userSession.sid, userSession.domain)
    });

    injector.register({
        provide: "MailboxesPersistence",
        factory: () => new MailboxesClient(userSession.sid, userSession.domain)
    });

    injector.register({
        provide: "OwnerSubscriptionsPersistence",
        factory: () => {
            return new OwnerSubscriptionsClient(userSession.sid, userSession.domain, userSession.userId);
        }
    });

    injector.register({
        provide: "UserSubscriptionPersistence",
        factory: () => {
            return new UserSubscriptionClient(userSession.sid, userSession.domain);
        }
    });

    injector.register({
        provide: "UserMailIdentitiesPersistence",
        factory: () => new UserMailIdentitiesClient(userSession.sid, userSession.domain, userSession.userId)
    });

    injector.register({
        provide: "UserPersistence",
        factory: () => new UserClient(userSession.sid, userSession.domain)
    });

    injector.register({
        provide: "VEventPersistence",
        factory: containerUid => new VEventClient(userSession.sid, containerUid)
    });
}

function initI18N() {
    // lang can be any of AvailableLanguages
    const lang = store.state.session.settings.remote.lang;

    Vue.mixin(InheritTranslationsMixin);

    let fallbackLang = "en";
    const navigatorLang = navigator.language;
    if (navigatorLang) {
        fallbackLang = navigator.language.split("-")[0];
    }
    const i18n = new VueI18n({
        locale: lang,
        fallbackLocale: fallbackLang,
        dateTimeFormats: generateDateTimeFormats(store.state.session.settings.remote.timeformat)
    });

    injector.register({
        provide: "i18n",
        use: i18n
    });

    return i18n;
}
