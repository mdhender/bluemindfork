import Vue from "vue";
import { extensions } from "@bluemind/extensions";
import { mailTipUtils } from "@bluemind/mail";

import DecoratedFileItem from "./components/MailAttachment/DecoratedFileItem.vue";
import PreviewBlockedRemoteContent from "./components/MailAttachment/Preview/Fallback/PreviewBlockedRemoteContent";
import PreviewTooLarge from "./components/MailAttachment/Preview/Fallback/PreviewTooLarge";

const { MailTipTypes } = mailTipUtils;

export default function () {
    Vue.component("decorated-file-item", DecoratedFileItem);
    Vue.component("PreviewBlockedRemoteContent", PreviewBlockedRemoteContent);
    Vue.component("PreviewTooLarge", PreviewTooLarge);

    extensions.register("webapp", "signature", {
        command: {
            name: "get-mail-tips",
            fn: ({ context }) => {
                context.filter.mailTips.push(MailTipTypes.SIGNATURE);
                return { context };
            }
        }
    });

    extensions.register("webapp.mail", "file-item", {
        component: {
            name: "decorated-file-item",
            path: "message.file",
            priority: 0
        }
    });

    extensions.register("webapp.mail", "mail-app", {
        component: {
            name: "PreviewBlockedRemoteContent",
            path: "file.preview",
            priority: 1
        }
    });

    extensions.register("webapp.mail", "mail-app", {
        component: {
            name: "PreviewTooLarge",
            path: "file.preview",
            priority: 2
        }
    });
}
