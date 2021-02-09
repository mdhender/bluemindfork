import MailActionsPanel from "../components/MailActionsPanel";
import MailApp from "../components/MailApp";
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
                path: ".t/:message",
                component: MailThread
            },
            { name: "mail:home", path: "", component: MailActionsPanel },

            ...virtualRoutes.map(route => Object.assign(route, { path: "" }))
        ]
    }
];
