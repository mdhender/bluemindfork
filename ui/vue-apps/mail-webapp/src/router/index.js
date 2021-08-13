import MailDefaultRightPanel from "../components/MailDefaultRightPanel";
import MailApp from "../components/MailApp";
import MailRouteMessage from "../components/MailRouteMessage";
import MailRouteConversation from "../components/MailRouteConversation";
import virtualRoutes from "./virtualRoutes";

export default [
    { path: "/index.html", redirect: "/mail/" },
    {
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
            { name: "mail:home", path: "", component: MailDefaultRightPanel },

            ...virtualRoutes.map(route => Object.assign(route, { path: "" }))
        ]
    }
];
