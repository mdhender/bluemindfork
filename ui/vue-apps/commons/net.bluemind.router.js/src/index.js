import global from "@bluemind/global";
import VueRouter from "vue-router";
import Vue from "vue";
import RelativeNavigationPlugin from "./RelativeNavigationPlugin";

RelativeNavigationPlugin.extends(VueRouter);
Vue.use(VueRouter);

const options = {
    base: new URL(document.baseURI).pathname.replace(/\/[^/]*$/, ""),
    mode: "history"
};

export default global.$router || (global.$router = new VueRouter(options));
