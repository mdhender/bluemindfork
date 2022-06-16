import MailDefaultRightPanel from "~/components/MailDefaultRightPanel";
import MailApp from "~/components/MailApp";
import MailRouteMessage from "~/components/MailRouteMessage";
import MailRouteConversation from "~/components/MailRouteConversation";

export default {
    name: "mail:root",
    path: "/mail/:messagequery*",
    component: MailApp,
    children: [
        {
            name: "mail:message",
            path: ".m/:messagepath",
            component: MailRouteMessage
        },
        {
            name: "mail:conversation",
            path: ".t/:conversationpath",
            component: MailRouteConversation
        },
        { name: "mail:home", path: "", component: MailDefaultRightPanel }
    ]
};
