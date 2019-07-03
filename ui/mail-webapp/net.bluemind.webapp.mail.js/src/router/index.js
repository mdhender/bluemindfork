import MailApp from "@bluemind/webapp.mail.ui.vuejs";
import { MailThread, MailMessageNew, MailMessageStarter } from "@bluemind/webapp.mail.ui.vuejs";

let actionsOnMailConsult = {
    // if folder value dont change, this function is not executed
    folder(store, value) {
        return store.dispatch("backend.mail/items/all", value)
            .then(() => 
                store.commit("backend.mail/folders/setCurrent", value)
            );
    },
    mail(store, value, unused, { params }) {
        return store.dispatch("backend.mail/items/select", { uid: value, folder: params.folder });
    }
};

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
                path: ":folder",
                component: MailMessageStarter,
                meta: {
                    $actions: {
                        folder(store, value) {
                            return store.dispatch("backend.mail/items/all", value)
                                .then(() => 
                                    store.commit("backend.mail/folders/setCurrent", value)
                                );
                        }
                    }
                }
            },
            {
                path: ":folder/:mail",
                component: MailThread,
                children: [
                    {
                        path:"reply",
                        name: "replyTo",
                        meta: {
                            $actions: actionsOnMailConsult
                        }
                    },
                    {
                        path:"replyAll",
                        name: "replyToAll",
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
