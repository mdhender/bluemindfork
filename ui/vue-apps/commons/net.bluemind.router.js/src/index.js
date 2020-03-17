import global from "@bluemind/global";
import VueRouter from "vue-router";
import Vue from "vue";
import RelativeNavigationPlugin from "./RelativeNavigationPlugin";

RelativeNavigationPlugin.extends(VueRouter);
Vue.use(VueRouter);

const options = {
    base: "/webapp",
    mode: "history"
};

export default global.$router || (global.$router = new VueRouter(options));
