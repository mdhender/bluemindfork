import Vue from "vue";
import { extensions } from "@bluemind/extensions";
import { mailTipUtils } from "@bluemind/mail";

import DecoratedFileItem from "./components/MailAttachment/DecoratedFileItem.vue";
import PreviewBlockedRemoteContent from "./components/MailAttachment/Preview/Fallback/PreviewBlockedRemoteContent";
import PreviewTooLarge from "./components/MailAttachment/Preview/Fallback/PreviewTooLarge";
import ForwardEventAlertTrigger from "./calendar/components/alerts/ForwardEventAlertTrigger";
import NotifyOrganizerTrigger from "./calendar/components/alerts/NotifyOrganizerTrigger";
import NotifyOrganizerAlert from "./calendar/components/alerts/NotifyOrganizerAlert";
import ForwardEventHandler from "./calendar/handlers/ForwardEventHandler";
import ForwardedEventAlert from "./calendar/components/alerts/ForwardedEventAlert";
import PrivateEventNotSentToDelegatesAlert from "./calendar/components/alerts/PrivateEventNotSentToDelegatesAlert";
import UsedQuotaAlert from "./components/MailAlerts//UsedQuotaAlert";

const { MailTipTypes } = mailTipUtils;

export default function () {
    extensions.register("webapp", "net.bluemind.webapp.mail.js", {
        command: {
            name: "get-mail-tips",
            fn: ({ context }) => {
                context.filter.mailTips.push(MailTipTypes.SIGNATURE);
                return { context };
            }
        }
    });

    Vue.component("DecoratedFileItem", DecoratedFileItem);
    extensions.register("webapp.mail", "net.bluemind.webapp.mail.js", {
        component: {
            name: "DecoratedFileItem",
            path: "message.file",
            priority: 0
        }
    });

    Vue.component("PreviewBlockedRemoteContent", PreviewBlockedRemoteContent);
    extensions.register("webapp.mail", "net.bluemind.webapp.mail.js", {
        component: {
            name: "PreviewBlockedRemoteContent",
            path: "file.preview",
            priority: 1
        }
    });

    Vue.component("PreviewTooLarge", PreviewTooLarge);
    extensions.register("webapp.mail", "net.bluemind.webapp.mail.js", {
        component: {
            name: "PreviewTooLarge",
            path: "file.preview",
            priority: 2
        }
    });
    Vue.component("ForwardedEventAlert", ForwardedEventAlert);
    Vue.component("PrivateEventNotSentToDelegatesAlert", PrivateEventNotSentToDelegatesAlert);
    Vue.component("UsedQuotaAlert", UsedQuotaAlert);
    Vue.component("NotifyOrganizerAlert", NotifyOrganizerAlert);
    Vue.component("NotifyOrganizerTrigger", NotifyOrganizerTrigger);
    extensions.register("webapp.mail", "net.bluemind.webapp.mail.js", {
        component: {
            path: "composer.header",
            name: "NotifyOrganizerTrigger"
        }
    });
    Vue.component("ForwardEventAlertTrigger", ForwardEventAlertTrigger);
    extensions.register("webapp.mail", "net.bluemind.webapp.mail.js", {
        component: {
            path: "viewer.header",
            name: "ForwardEventAlertTrigger"
        }
    });
    extensions.register("webapp", "net.bluemind.webapp.mail.js", {
        command: {
            name: "forward",
            fn: ForwardEventHandler,
            after: true
        }
    });
}
