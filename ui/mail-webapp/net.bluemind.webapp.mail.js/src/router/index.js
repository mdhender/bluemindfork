import MailApp, { MailThread, MailMessageNew, MailMessageStarter } from "@bluemind/webapp.mail.ui.vuejs";
import ContainerObserver from "@bluemind/containerobserver";

function observeContainer(store, value) {
    const previous = store.getters["backend.mail/folders/currentFolder"];
    if (previous) {
        ContainerObserver.forget("mailbox_records", previous);
    }
    ContainerObserver.observe("mailbox_records", value);
}

const actionsOnMailConsult = {
    // if folder value dont change, this function is not executed
    folder(store, value) {
        return store.dispatch("backend.mail/items/all", value).then(() => {
            observeContainer(store, value);
            return store.commit("backend.mail/folders/setCurrent", value);
        });
    },
    mail(store, value, unused, { params }) {
        store.commit("backend.mail/items/setAttachments", []);
        return store.dispatch("backend.mail/items/select", { uid: value, folder: params.folder });
    }
};

function actionOnSearch(store, value) {
    const folderUid = store.state["backend.mail/folders"].settings.current;
    return store.dispatch("backend.mail/items/search", { folderUid, pattern: value });
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
                        mail(store, value) {
                            const folderUid = store.state["backend.mail/folders"].settings.current;
                            return store.dispatch(
                                "backend.mail/items/select", 
                                { uid: value, folder: folderUid }
                            );
                        }
                    }
                }
            },
            {
                path: ":folder",
                component: MailMessageStarter,
                meta: {
                    $actions: {
                        folder(store, value) {
                            store.commit("backend.mail/items/setSearchPattern", null);
                            store.commit("backend.mail/items/setSearchLoading", null);
                            return store.dispatch("backend.mail/items/all", value).then(() => {
                                observeContainer(store, value);
                                return store.commit("backend.mail/folders/setCurrent", value);
                            });
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
