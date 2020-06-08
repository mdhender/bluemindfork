import { BmModalPlugin } from "@bluemind/styleguide";
import { extend } from "@bluemind/vuex-router";
import { FirstDayOfWeek, InheritTranslationsMixin } from "@bluemind/i18n";
import { sync } from "vuex-router-sync";
import AlertStore from "@bluemind/alert.store";
import injector from "@bluemind/inject";
import MainApp from "./MainApp.vue";
import NotificationManager from "./NotificationManager";
import router from "@bluemind/router";
import store from "@bluemind/store";
import Vue from "vue";
import Vue2TouchEvents from "vue2-touch-events";
import VueBus from "@bluemind/vue-bus";
import VueI18n from "vue-i18n";
import VueSockjsPlugin from "@bluemind/vue-sockjs";
import WebsocketClient from "@bluemind/sockjs";

initWebApp();

function initWebApp() {
    registerUserSession();
    const userSession = injector.getProvider("UserSession").get();
    registerDependencies(userSession);
    initStore();
    setVuePlugins();
    setNotificationWhenReceivingMail(userSession);
    const i18n = initI18N(userSession);

    new Vue({
        el: "#app",
        i18n,
        render: h => h(MainApp),
        router,
        store
    });
}

function setVuePlugins() {
    Vue.use(VueI18n);
    Vue.use(VueBus, store);
    Vue.use(VueSockjsPlugin, VueBus);
    Vue.use(Vue2TouchEvents, { disableClick: true });
    Vue.use(BmModalPlugin);
}

function setNotificationWhenReceivingMail(userSession) {
    if (userSession.roles.includes("hasMail")) {
        const notificationManager = new NotificationManager();

        const mailAppExtension = window.bmExtensions_["net.bluemind.banner"].find(
            extension => extension.application.role === "hasMail" && extension.application.href.includes("mail")
        );
        const mailIconAsSvg = mailAppExtension.application.children["icon-svg"].body;
        const mailIconAsBlobURL = URL.createObjectURL(new Blob([mailIconAsSvg], { type: "image/svg+xml" }));

        const address = userSession.userId + ".notifications.mails";

        const sendNotification = ({ data }) => {
            const mailSubject = data.body.body;
            const mailSender = data.body.title;
            notificationManager.send(mailSender, mailSubject, mailIconAsBlobURL);
        };

        new WebsocketClient().register(address, sendNotification);
    }
}

function registerUserSession() {
    injector.register({
        provide: "UserSession",
        use: window.bmcSessionInfos
    });
}

function initStore() {
    sync(store, router);
    extend(router, store);
    store.registerModule("alert", AlertStore);
}

function registerDependencies(userSession) {
    // if no lang defined, use monday as fdow
    const firstDayOfWeek = FirstDayOfWeek[userSession.lang.toUpperCase()] || 1;

    injector.register({
        provide: "Environment",
        use: { firstDayOfWeek }
    });

    injector.register({
        provide: "GlobalEventBus",
        use: VueBus.Client
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
    const formats = {
        short_date: {
            day: "2-digit",
            month: "2-digit",
            year: "numeric"
        },
        short_time: {
            hour: "2-digit",
            minute: "2-digit"
        },
        relative_date: {
            weekday: "short",
            day: "2-digit",
            month: "2-digit"
        },
        full_date: {
            weekday: "short",
            day: "2-digit",
            month: "2-digit",
            year: "numeric"
        },
        full_date_time: {
            weekday: "long",
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        }
    };

    return {
        fr: formats,
        en: formats
    };
}
