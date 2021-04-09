import { mapActions, mapState } from "vuex";
import { REMOVE, WARNING } from "@bluemind/alert.store";

export default {
    data() {
        return { NEED_APP_RELOAD_UID: "NEED_APP_RELOAD", NEED_RECONNECTION_UID: "NEED_RECONNECTION" };
    },
    computed: {
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "pref-right-panel") })
    },
    methods: {
        ...mapActions("alert", { REMOVE, WARNING }),
        showReloadAppAlert() {
            // reconnection alert has priority over APP_RELOAD
            const alreadyShown = this.alerts.find(
                alert => alert.uid === this.NEED_APP_RELOAD_UID || alert.uid === this.NEED_RECONNECTION_UID
            );

            if (!alreadyShown) {
                const alert = {
                    alert: { name: "preferences.NEED_APP_RELOAD", uid: this.NEED_APP_RELOAD_UID },
                    options: { area: "pref-right-panel", renderer: "ReloadAppAlert" }
                };
                this.WARNING(alert);
            }
        },
        showReconnectionAlert() {
            const isReloadAppAlertShown = this.alerts.find(alert => alert.uid === this.NEED_APP_RELOAD_UID);
            if (isReloadAppAlertShown) {
                this.REMOVE(isReloadAppAlertShown);
            }

            const isReconnectionAlertShown = this.alerts.find(alert => alert.uid === this.NEED_RECONNECTION_UID);
            if (!isReconnectionAlertShown) {
                const alert = {
                    alert: { name: "preferences.NEED_RECONNECTION", uid: this.NEED_RECONNECTION_UID },
                    options: { area: "pref-right-panel", renderer: "NeedReconnectionAlert" }
                };
                this.WARNING(alert);
            }
        }
    }
};
