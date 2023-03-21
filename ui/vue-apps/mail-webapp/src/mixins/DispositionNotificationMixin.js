import { INFO, ERROR, REMOVE } from "@bluemind/alert.store";
import { EmailExtractor, Flag } from "@bluemind/email";
import { draftUtils, messageUtils } from "@bluemind/mail";
import { CURRENT_MAILBOX, MY_OUTBOX } from "~/getters";
import { ADD_FLAG } from "~/actions";
import sendTemplate from "~/utils/eml-templates/sendTemplate";
import MDNTemplate from "~/utils/eml-templates/templates/MDNTemplate";

export default {
    data() {
        return {
            dispositionNotificationAlerts: [],
            messagesWithMDNRequestConsumer: (messages, fn) => {
                messages.forEach(message => {
                    if (message.headers?.length) {
                        const index = messageUtils.findDispositionNotificationHeaderIndex(message.headers);
                        if (index >= 0 && !message.flags?.includes(Flag.MDN_SENT)) {
                            fn(message, message.headers[index]);
                        }
                    }
                });
            }
        };
    },
    destroyed() {
        this.hideDispositionNotificationAlert();
    },
    methods: {
        handleDispositionNotification(messages) {
            switch (this.$store.state.settings.answer_read_confirmation) {
                case "always":
                    this.sendDispositionNotifications(messages);
                    break;
                case "never":
                    this.ignoreDispositionNotificationRequests(messages);
                    break;
                case "ask":
                default:
                    this.showDispositionNotificationAlerts(messages);
                    break;
            }
        },
        showDispositionNotificationAlerts(messages) {
            const outbox = this.$store.getters[`mail/${MY_OUTBOX}`];
            const from = this.computeFrom();
            this.messagesWithMDNRequestConsumer(messages, (message, dispositionNotificationToHeader) => {
                dispositionNotificationToHeader.values.forEach(to => {
                    const uid = dispositionNotificationAlertUid(message, to);
                    this.$store.dispatch(`alert/${INFO}`, {
                        alert: {
                            name: "mail.mdn_request",
                            uid,
                            payload: {
                                to: { dn: EmailExtractor.extractDN(to), address: EmailExtractor.extractEmail(to) },
                                from,
                                message,
                                outbox
                            }
                        },
                        options: { area: "right-panel", renderer: "DispositionNotification", dismissible: false }
                    });
                    this.dispositionNotificationAlerts.push(uid);
                });
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
        },
        ignoreDispositionNotificationRequests(messages) {
            this.messagesWithMDNRequestConsumer(messages, async message => {
                await this.$store.dispatch(`mail/${ADD_FLAG}`, { messages: [message], flag: Flag.MDN_SENT });
            });
        },
        sendDispositionNotifications(messages) {
            const outbox = this.$store.getters[`mail/${MY_OUTBOX}`];
            const from = this.computeFrom();
            this.messagesWithMDNRequestConsumer(messages, async (message, dispositionNotificationToHeader) => {
                dispositionNotificationToHeader.values.forEach(async to => {
                    try {
                        const toAddress = EmailExtractor.extractEmail(to);
                        await sendTemplate({
                            template: MDNTemplate,
                            parameters: {
                                subject: message.subject,
                                date: message.date.toLocaleString(),
                                from: from.dn ? `${from.dn} <${from.address}>` : from.address,
                                to,
                                toAddress,
                                messageId: message.messageId
                            },
                            from,
                            to: { dn: EmailExtractor.extractDN(to), address: toAddress },
                            outboxUid: outbox.remoteRef.uid,
                            additionalHeaders: [
                                { name: messageUtils.MessageHeader.REFERENCES, values: [message.messageId] }
                            ]
                        });
                        await this.$store.dispatch(`mail/${ADD_FLAG}`, { messages: [message], flag: Flag.MDN_SENT });
                    } catch (e) {
                        this.$store.dispatch(`alert/${ERROR}`, {
                            alert: { name: "mail.mdn_sent", uid: "MDN_SENT_UID", payload: { recipient: to } }
                        });
                        throw e;
                    }
                });
            });
        }
    }
};

function dispositionNotificationAlertUid(message, to) {
    return `DISPOSITION_NOTIFICATION-${message.key}-${to}`;
}
