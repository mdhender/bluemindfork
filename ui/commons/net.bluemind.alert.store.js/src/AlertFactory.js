import Alert from "./Alert";
import deepClone from "lodash.clonedeep";
import injector from "@bluemind/inject";

export default {
    
    alerts : {},

    register(alerts) {
        Object.assign(this.alerts, alerts);
    },

    create(code, props, uid) {
        const vueI18n = injector.getProvider("i18n").get();

        const alert = deepClone(this.alerts[code]);
        alert.code = code;
        alert.props = props;
        alert.uid = uid;
        alert.message = vueI18n.t(alert.key, props);

        return new Alert(alert);
    }
};