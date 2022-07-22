import global from "@bluemind/global";
import vuexPlugin from "@bluemind/webappdata";
import BmVuexStore from "./BmVuexStore";
import Vuex from "vuex";
import Vue from "vue";

Vue.use(Vuex);

export default global.$store || (global.$store = new BmVuexStore({ plugins: [vuexPlugin] }));
