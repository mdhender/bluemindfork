import MailActionsPanel from "../components/MailActionsPanel";
import MailApp from "../components/MailApp";
import MailMessage from "../components/MailThread/MailMessage";
import MailThread from "../components/MailThread/MailThread";
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
                component: MailMessage
            },
            {
                name: "mail:conversation",
                path: ".t/:conversationpath",
                component: MailThread
            },
            { name: "mail:home", path: "", component: MailActionsPanel },

            ...virtualRoutes.map(route => Object.assign(route, { path: "" }))
        ]
    }
];
