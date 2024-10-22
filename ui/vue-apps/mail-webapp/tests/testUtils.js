import merge from "lodash.merge";
import Vuex from "vuex";
import VueRouter from "vue-router";
import { createLocalVue, mount } from "@vue/test-utils";

import AlertStore from "@bluemind/alert.store";
import inject from "@bluemind/inject";
import { loadingStatusUtils, messageUtils } from "@bluemind/mail";

import MailAppStore from "../src/store";
const { messageKey: generateKey } = messageUtils;
const { LoadingStatus } = loadingStatusUtils;

const localVue = createLocalVue();
localVue.use(Vuex);
VueRouter.prototype.relative = function () {};
VueRouter.prototype.navigate = function () {};
localVue.use(VueRouter);

const folderUid = "folder:uid";
export const conversationKey = generateKey(1, folderUid);
export const messageKey = generateKey(1, folderUid);
const userId = "6793466E-F5D4-490F-97BF-DF09D3327BF4";

inject.register({
    provide: "UserSession",
    use: { userId }
});

const mailbox = {
    key: "MY_MAILBOX",
    type: "users",
    owner: userId,
    name: "bluemind",
    writable: true,
    remoteRef: {
        uid: "uid"
    }
};

const fakedSettingsStore = {
    namespaced: true,
    state: { mail_thread: "false" }
};

export function createStore() {
    const store = new Vuex.Store();
    store.registerModule("alert", AlertStore);
    store.registerModule("mail", MailAppStore);
    store.registerModule("settings", fakedSettingsStore);
    store.commit("mail/SET_MAX_MESSAGE_SIZE", 10);
    store.commit("mail/ADD_MAILBOXES", [mailbox]);
    store.commit("mail/ADD_FOLDER", {
        key: folderUid,
        imapName: "fol",
        mailboxRef: { key: "MY_MAILBOX" },
        writable: true,
        path: "my/folder"
    });
    store.commit("mail/ADD_FOLDER", {
        key: "InboxKey",
        imapName: "INBOX",
        path: "INBOX",
        default: true,
        mailboxRef: { key: "MY_MAILBOX" },
        writable: true
    });
    store.commit("mail/ADD_FOLDER", {
        key: "TraskKey",
        imapName: "Trash",
        path: "Trash",
        default: true,
        mailboxRef: { key: "MY_MAILBOX" },
        writable: true
    });
    store.commit("mail/ADD_FOLDER", {
        key: "SentKey",
        imapName: "Sent",
        path: "Sent",
        default: true,
        mailboxRef: { key: "MY_MAILBOX" },
        writable: true
    });
    store.commit("mail/ADD_FOLDER", {
        key: "tpl",
        imapName: "Templates",
        path: "Templates",
        default: true,
        mailboxRef: { key: "MY_MAILBOX" },
        writable: true
    });
    store.commit("mail/ADD_FOLDER", {
        key: "drf",
        imapName: "Drafts",
        path: "Drafts",
        default: true,
        mailboxRef: { key: "MY_MAILBOX" },
        writable: true
    });
    const conversations = [
        {
            key: conversationKey,
            flags: [],
            folderRef: { key: folderUid, uid: folderUid },
            remoteRef: { uid: "a1b2c3d4e5f6" },
            messages: [messageKey],
            date: new Date(123456),
            loading: LoadingStatus.LOADED
        }
    ];
    const messages = [
        {
            key: messageKey,
            folderRef: { key: folderUid, uid: folderUid },
            conversationRef: { key: conversationKey },
            remoteRef: { internalId: 17 }
        }
    ];
    store.commit("mail/ADD_CONVERSATIONS", { conversations });
    store.commit("mail/ADD_MESSAGES", { messages });

    store.commit("mail/SET_CURRENT_CONVERSATION", conversations[0]);
    store.commit("mail/SET_ACTIVE_FOLDER", { key: folderUid });

    return store;
}

export function createWrapper(component, overrides, propsData = {}) {
    const defaultMountingOptions = {
        localVue,
        store: createStore(),
        router: new VueRouter(),
        propsData: propsData,
        mocks: {
            $t: () => "",
            $tc: () => "",
            $te: () => false,
            $d: () => "",
            $i18n: {
                t: () => "",
                tc: () => "",
                te: () => false,
                d: () => ""
            }
        }
    };
    const mergedMountingOptions = merge(defaultMountingOptions, overrides);
    return mount(component, mergedMountingOptions);
}
