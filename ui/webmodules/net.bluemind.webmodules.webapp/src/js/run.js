import Vue from "vue";
import MainApp from "./MainApp.vue";
import router from "@bluemind/router";
import injector from "@bluemind/inject";
import store from "@bluemind/store";
import { sync } from "vuex-router-sync";
import { extend } from "@bluemind/vuex-router";
import VueI18n from "vue-i18n";
import { InheritTranslationsMixin } from "@bluemind/i18n";

Vue.use(VueI18n);

injector.register({
    provide: "UserSession",
    use: window.bmcSessionInfos
});

const userSession = injector.getProvider("UserSession").get();

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