import injector from "@bluemind/inject";
import MailActionsPanel from "../components/MailActionsPanel";
import MailApp from "../components/MailApp";
import MailComposer from "../components/MailComposer";
import MailThread from "../components/MailThread/MailThread";
import MessageQueryParam from "./MessageQueryParam";
import virtualRoutes from "./virtualRoutes";

export default [
    {
        name: "mail:root",
        path: "/mail/:messagequery*",
        component: MailApp,
        meta: {
            onEnter: store => store.dispatch("mail-webapp/bootstrap", injector.getProvider("UserSession").get().userId),
            watch: {
                messagequery: (store, value) =>
                    store.dispatch("mail-webapp/loadMessageList", MessageQueryParam.parse(value))
            }
        },
        children: [
            {
                name: "mail:new",
                path: "new",
                component: MailComposer
            },
            {
                name: "mail:message",
                path: ".t/:message",
                component: MailThread,
                meta: {
                    onUpdate: (store, to) => store.dispatch("mail-webapp/selectMessage", to.params.message),
                    onLeave: store => store.commit("mail-webapp/currentMessage/clear")
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

            ...virtualRoutes.map(route => Object.assign(route, { path: "" }))
        ]
    }
];
