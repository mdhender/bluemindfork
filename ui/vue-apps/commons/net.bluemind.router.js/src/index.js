import global from "@bluemind/global";
import RouterMixin from "./RouterMixin";
import VueRouter from "vue-router";
import Vue from "vue";

const options = {
    base: "/webapp",
    mode: "history"
};
Vue.use(VueRouter);

export default global.$router || (global.$router = new VueRouter(options));
export { RouterMixin };
