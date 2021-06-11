import { mapActions, mapState } from "vuex";
import { ERROR, LOADING, REMOVE, SUCCESS, WARNING } from "@bluemind/alert.store";

export default {
    data() {
        return {
            NEED_APP_RELOAD_UID: "NEED_APP_RELOAD",
            NEED_RECONNECTION_UID: "NEED_RECONNECTION",
            SYNC_CALENDAR_IN_PROGRESS_UID: "SYNC_CALENDAR_IN_PROGRESS",
            SYNC_CALENDAR_SUCCESS_UID: "SYNC_CALENDAR_SUCCESS",
            SYNC_CALENDAR_ERROR_UID: "SYNC_CALENDAR_ERROR"
        };
    },
    computed: {
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "pref-right-panel") })
    },
    methods: {
        ...mapActions("alert", { ERROR, LOADING, REMOVE, SUCCESS, WARNING }),
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
        },
        showSyncCalendarInProgress() {
            const alert = {
                alert: {
                    name: "preferences." + this.SYNC_CALENDAR_IN_PROGRESS_UID,
                    uid: this.SYNC_CALENDAR_IN_PROGRESS_UID
                },
                options: { area: "pref-right-panel", renderer: "DefaultAlert" }
            };
            this.LOADING(alert);
        },
        removeSyncCalendarInProgress() {
            const inProgressAlert = this.alerts.find(alert => alert.uid === this.SYNC_CALENDAR_IN_PROGRESS_UID);
            if (inProgressAlert) {
                this.REMOVE(inProgressAlert);
            }
        },
        showSyncCalendarSuccess() {
            this.removeSyncCalendarInProgress();
            const alert = {
                alert: { name: "preferences." + this.SYNC_CALENDAR_SUCCESS_UID, uid: this.SYNC_CALENDAR_SUCCESS_UID },
                options: { area: "pref-right-panel", renderer: "DefaultAlert" }
            };
            this.SUCCESS(alert);
        },
        showSyncCalendarError() {
            this.removeSyncCalendarInProgress();
            const alert = {
                alert: { name: "preferences." + this.SYNC_CALENDAR_ERROR_UID, uid: this.SYNC_CALENDAR_ERROR_UID },
                options: { area: "pref-right-panel", renderer: "DefaultAlert" }
            };
            this.ERROR(alert);
        }
    }
};
