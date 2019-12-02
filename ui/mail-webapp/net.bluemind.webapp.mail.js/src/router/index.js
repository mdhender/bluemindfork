import MailApp, { MailThread, MailMessageNew, MailMessageStarter } from "@bluemind/webapp.mail.ui.vuejs";

const actionsOnMailConsult = {
    folder: (store, value) => store.dispatch("mail-webapp/selectFolder", value),
    mail: (store, value) => store.dispatch("mail-webapp/selectMessage", value)
};

function actionOnSearch(store, value) {
    return store.dispatch("mail-webapp/search", value);
}

export default [
    {
        path: "/mail/",
        component: MailApp,
        children: [
            {
                path: "new",
                component: MailMessageNew,
                name: "newMessage",
                alias: ":folder/new"
            },
            {
                path: "search/:pattern",
                component: MailMessageStarter,
                name: "search",
                meta: {
                    $actions: {
                        pattern: actionOnSearch
                    }
                }
            },
            {
                path: "search/:pattern/:mail",
                name: "searchItemResult",
                component: MailThread,
                meta: {
                    $actions: {
                        pattern: actionOnSearch,
                        mail: (store, value) => store.dispatch("mail-webapp/selectMessage", value)
                    }
                }
            },
            {
                path: ":folder/",
                name: "folder",
                component: MailMessageStarter,
                meta: {
                    $actions: {
                        folder: {
                            call: (store, value) => store.dispatch("mail-webapp/selectFolder", value),
                            force: true
                        }
                    }
                }
            },
            {
                path: ":folder/:mail",
                component: MailThread,
                children: [
                    {
                        path: "reply",
                        name: "replyTo",
                        meta: {
                            $actions: actionsOnMailConsult
                        }
                    },
                    {
                        path: "replyAll",
                        name: "replyToAll",
                        meta: {
                            $actions: actionsOnMailConsult
                        }
                    },
                    {
                        path: "forward",
                        name: "forwardTo",
                        meta: {
                            $actions: actionsOnMailConsult
                        }
                    },
                    {
                        path: "",
                        meta: {
                            $actions: actionsOnMailConsult
                        }
                    }
                ]
            },
            {
                path: "",
                component: MailMessageStarter
            }
        ]
    }
];
