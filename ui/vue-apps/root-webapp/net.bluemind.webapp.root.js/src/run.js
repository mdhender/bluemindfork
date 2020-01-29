import { extend } from "@bluemind/vuex-router";
import { FirstDayOfWeek } from "@bluemind/i18n";
import { InheritTranslationsMixin } from "@bluemind/i18n";
import { sync } from "vuex-router-sync";
import injector from "@bluemind/inject";
import MainApp from "./MainApp.vue";
import router from "@bluemind/router";
import store from "@bluemind/store";
import Vue from "vue";
import VueBus from "@bluemind/vue-bus";
import VueI18n from "vue-i18n";
import VueSockjsPlugin from "@bluemind/vue-sockjs";

setVuePlugins();

injector.register({
    provide: "UserSession",
    use: window.bmcSessionInfos
});
const userSession = injector.getProvider("UserSession").get();

registerDependencies(userSession);

sync(store, router);
extend(router, store);

Vue.mixin(InheritTranslationsMixin);

const i18n = new VueI18n({ locale: userSession.lang, fallbackLocale: 'en', dateTimeFormats: getDateTimeFormats() });

injector.register({
    provide: "i18n",
    use: i18n
});

new Vue({
    el: "#app",
    i18n,
    render: h => h(MainApp),
    router,
    store
});

function setVuePlugins() {
    Vue.use(VueI18n);
    Vue.use(VueBus, { store });
    Vue.use(VueSockjsPlugin, { url: '/eventbus/', VueBus });
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
        use: VueBus
    });
}

function getDateTimeFormats() {
    const formats = {
        short_date: {
            day: '2-digit', month: '2-digit', year: 'numeric'
        },
        short_time: {
            hour: '2-digit', minute: '2-digit'
        },
        relative_date: {
            weekday: 'short', day: '2-digit', month: '2-digit'
        },
        full_date: {
            weekday: 'short', day: '2-digit', month: '2-digit', year: 'numeric'
        },
        full_date_time: {
            weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
        }
    };
    
    return {
        'fr': formats,
        'en': formats
    };
}
//Ajouter des data via des plugins
//Ajouter des plugin vue via des plugins
