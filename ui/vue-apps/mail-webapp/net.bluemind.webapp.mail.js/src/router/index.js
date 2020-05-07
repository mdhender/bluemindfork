import MailApp, { MailThread, MailMessageNew, MailActionsPanel } from "@bluemind/webapp.mail.ui.vuejs";
import virtualRoutes from "./virtualRoutes";
import MessageQueryParam from "./MessageQueryParam";
import injector from "@bluemind/inject";

export default [
    {
        name: "mail:root",
        path: "/mail/:messagequery*",
        component: MailApp,
        meta: {
            onEnter(store) {
                return store.dispatch("mail-webapp/bootstrap", injector.getProvider("UserSession").get().login);
            },
            watch: {
                messagequery: (store, value) => {
                    return store.dispatch("mail-webapp/loadMessageList", MessageQueryParam.parse(value));
                }
            }
        },
        children: [
            {
                name: "mail:new",
                path: "new",
                component: MailMessageNew
            },
            {
                name: "mail:message",
                path: ".t/:message",
                component: MailThread,
                meta: {
                    onUpdate: (store, to) => store.dispatch("mail-webapp/selectMessage", to.params.message)
                },
                children: [
                    {
                        name: "mail:reply",
                        path: "reply"
                    },
                    {
                        name: "mail:replyAll",
                        path: "replyAll"
                    },
                    {
                        name: "mail:forward",
                        path: "forward"
                    }
                ]
            },
            { name: "mail:home", path: "", component: MailActionsPanel },

            // Virtual routes
            ...virtualRoutes.map(route => Object.assign(route, { path: "" }))
        ]
    }
];
