import { inject } from "@bluemind/inject";
import { AlertTypes } from "@bluemind/alert.store";
import { INFO, ERROR, WARNING } from "@bluemind/alert.store";
import SystemAlert from "../components/alerts/SystemAlert";
import store from "@bluemind/store";

export function useAnnouncementsPersistence() {
    let userAnnouncementsPersistence = [];

    async function annoucementsPersistence() {
        userAnnouncementsPersistence = await inject("UserAnnouncementsPersistence").get();
        userAnnouncementsPersistence.forEach((announcement, index) => {
            let alert = {
                alert: {
                    name: "announcement",
                    type: getAlertType(announcement.kind),
                    uid: index,
                    payload: announcement
                },
                options: { area: "system-alert", renderer: SystemAlert }
            };
            switch (announcement.kind) {
                case "Error":
                    store.dispatch(`alert/${ERROR}`, alert);
                    break;
                case "Info":
                    store.dispatch(`alert/${INFO}`, alert);
                    break;
                case "Warn":
                    store.dispatch(`alert/${WARNING}`, alert);
                    break;
            }
        });
    }

    return { annoucementsPersistence };
}

function getAlertType(kind) {
    switch (kind) {
        case "Error":
            return AlertTypes.ERROR;
        case "Info":
            return AlertTypes.INFO;
        case "Warn":
            return AlertTypes.WARNING;
    }
}
