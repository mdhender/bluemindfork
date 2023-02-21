import { INFO, REMOVE } from "@bluemind/alert.store";
import { EmailExtractor, Flag } from "@bluemind/email";
import { draftUtils, messageUtils } from "@bluemind/mail";
import { CURRENT_MAILBOX, MY_OUTBOX } from "~/getters";

export default {
    data() {
        return { dispositionNotificationAlerts: [] };
    },
    destroyed() {
        this.hideDispositionNotificationAlert();
    },
    methods: {
        showDispositionNotificationAlert(messages) {
            const outbox = this.$store.getters[`mail/${MY_OUTBOX}`];
            const from = this.computeFrom();
            messages.forEach(message => {
                if (message.headers?.length) {
                    const index = messageUtils.findDispositionNotificationHeaderIndex(message.headers);
                    if (index >= 0 && !message.flags?.includes(Flag.MDN_SENT)) {
                        const header = message.headers[index];
                        header.values.forEach(to => {
                            const uid = dispositionNotificationAlertUid(message, to);
                            this.$store.dispatch(`alert/${INFO}`, {
                                alert: {
                                    name: "mail.mdn_request",
                                    uid,
                                    payload: {
                                        to: {
                                            dn: EmailExtractor.extractDN(to),
                                            address: EmailExtractor.extractEmail(to)
                                        },
                                        from,
                                        message,
                                        outbox
                                    }
                                },
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
        },
        computeFrom() {
            const defaultIdentity = this.$store.getters["root-app/DEFAULT_IDENTITY"];
            const identity =
                this.$store.state.settings.auto_select_from === "replies_and_new_messages"
                    ? draftUtils.findIdentityFromMailbox(
                          this.$store.getters["mail/" + CURRENT_MAILBOX],
                          this.$store.state["root-app"].identities,
                          defaultIdentity
                      )
                    : defaultIdentity;
            return { dn: identity.displayname, address: identity.email };
        }
    }
};

function dispositionNotificationAlertUid(message, to) {
    return `DISPOSITION_NOTIFICATION-${message.key}-${to}`;
}
