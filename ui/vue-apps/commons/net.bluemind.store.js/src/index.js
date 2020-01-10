import global from "@bluemind/global";
import Vuex from "vuex";
import Vue from "vue";

Vue.use(Vuex);

export default global.$store || (global.$store = new Vuex.Store());
