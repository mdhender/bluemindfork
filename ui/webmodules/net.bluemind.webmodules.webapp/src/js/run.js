import { extend } from "@bluemind/vuex-router";
import { FirstDayOfWeek } from "@bluemind/i18n";
import { InheritTranslationsMixin } from "@bluemind/i18n";
import { sync } from "vuex-router-sync";
import injector from "@bluemind/inject";
import MainApp from "./MainApp.vue";
import router from "@bluemind/router";
import store from "@bluemind/store";
import Vue from "vue";
import VueI18n from "vue-i18n";

Vue.use(VueI18n);

injector.register({
    provide: "UserSession",
    use: window.bmcSessionInfos
});

const userSession = injector.getProvider("UserSession").get();
const firstDayOfWeek = FirstDayOfWeek[userSession.lang.toUpperCase()] || 1; // if no lang defined, use monday as fdow

injector.register({
    provide: "Environment",
    use: {
        firstDayOfWeek: firstDayOfWeek // FIXME : use user settings instead
    }
});

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

//Ajouter des data via des plugins
//Ajouter des plugin vue via des plugins