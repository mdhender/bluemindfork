import MailActionsPanel from "../components/MailActionsPanel";
import MailApp from "../components/MailApp";
import MailThread from "../components/MailThread/MailThread";
import MessageQueryParam from "./MessageQueryParam";
import virtualRoutes from "./virtualRoutes";

export default [
    { path: "/index.html", redirect: "/mail/" },
    {
        name: "mail:root",
        path: "/mail/:messagequery*",
        component: MailApp,
        meta: {
            watch: {
                messagequery: (store, value) =>
                    store.dispatch("mail-webapp/loadMessageList", MessageQueryParam.parse(value))
            }
        },
        children: [
            {
                name: "mail:message",
                path: ".t/:message",
                component: MailThread,
                meta: {
                    onUpdate: (store, to, from, next) => {
                        store.dispatch("mail-webapp/selectMessage", to.params.message).catch(e => {
                            console.error("Unable to select message.", e);
                            next({ path: "/mail" });
                        });
                    },
                    onLeave: store => store.commit("mail-webapp/currentMessage/clear")
                }
            },
            { name: "mail:home", path: "", component: MailActionsPanel },

            ...virtualRoutes.map(route => Object.assign(route, { path: "" }))
        ]
    }
];
