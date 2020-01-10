import VuexProxy from "./VuexProxy";
import VueProxy from "./VueProxy";

export function install(Vue, store) {
    if (_Bus !== undefined) return;
    _Bus = new Vue();
    Vue.use(VueProxy, this);
    if (store) {
        this.use(VuexProxy, store);
    }
}

export let _Bus;
