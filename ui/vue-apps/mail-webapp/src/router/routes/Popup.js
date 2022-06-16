import MailPopupApp from "~/components/MailPopupApp";
import MailRouteMessage from "~/components/MailRouteMessage";
import MailRouteConversation from "~/components/MailRouteConversation";
import MailPopupCloseScreen from "~/components/MailPopupCloseScreen";

export default {
    name: "mail:popup",
    path: "/mail/popup/:messagequery*",
    component: MailPopupApp,
    alias: ["mail:popup:root", "mail:popup:home"],
    children: [
        {
            name: "mail:popup:message",
            path: ".m/:messagepath",
            component: MailRouteMessage
        },
        {
            name: "mail:popup:conversation",
            path: ".t/:conversationpath",
            component: MailRouteConversation
        },
        {
            name: "mail:popup:home",
            path: "",
            component: MailPopupCloseScreen
        }
    ]
};
