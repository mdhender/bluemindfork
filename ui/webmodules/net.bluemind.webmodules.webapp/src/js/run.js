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

new Vue({
    el: "#app",
    i18n: { locale: userSession.lang, fallbackLocale: 'en' },
    render: h => h(MainApp),
    router,
    store
});

function setVuePlugins() {
    Vue.use(VueI18n);
    Vue.use(VueBus, { store });
    Vue.use(VueSockjsPlugin, {url: '/eventbus/', VueBus});
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
//Ajouter des data via des plugins
//Ajouter des plugin vue via des plugins
