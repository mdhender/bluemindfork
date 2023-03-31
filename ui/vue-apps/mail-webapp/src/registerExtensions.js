import Vue from "vue";
import { extensions } from "@bluemind/extensions";
import { mailTipUtils } from "@bluemind/mail";

import DecoratedFileItem from "./components/MailAttachment/DecoratedFileItem.vue";
import PreviewBlockedRemoteContent from "./components/MailAttachment/Preview/Fallback/PreviewBlockedRemoteContent";
import PreviewTooLarge from "./components/MailAttachment/Preview/Fallback/PreviewTooLarge";

const { MailTipTypes } = mailTipUtils;

export default function () {
    extensions.register("webapp", "signature", {
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
}
