import { INFO, REMOVE } from "@bluemind/alert.store";
import { Flag } from "@bluemind/email";
import { messageUtils } from "@bluemind/mail";

export default {
    data() {
        return { dispositionNotificationAlerts: [] };
    },
    destroyed() {
        this.hideDispositionNotificationAlert();
    },
    methods: {
        showDispositionNotificationAlert(messages) {
            messages.forEach(message => {
                if (message.headers?.length) {
                    const index = messageUtils.findDispositionNotificationHeaderIndex(message.headers);
                    if (index >= 0 && !message.flags?.includes(Flag.MDN_SENT)) {
                        const header = message.headers[index];
                        header.values.forEach(to => {
                            const uid = dispositionNotificationAlertUid(message, to);
                            this.$store.dispatch(`alert/${INFO}`, {
                                alert: { uid, payload: { to, message } },
                                options: {
                                    area: "right-panel",
                                    renderer: "DispositionNotification",
                                    dismissible: false
                                }
                            });
                            this.dispositionNotificationAlerts.push(uid);
                        });
                    }
                }
            });
        },

        hideDispositionNotificationAlert() {
            let uid;
            while ((uid = this.dispositionNotificationAlerts.pop())) {
                this.$store.dispatch(`alert/${REMOVE}`, { uid });
            }
        }
    }
};

function dispositionNotificationAlertUid(message, to) {
    return `DISPOSITION_NOTIFICATION-${message.key}-${to}`;
}
