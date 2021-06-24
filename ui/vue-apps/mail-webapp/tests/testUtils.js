import merge from "lodash.merge";
import Vuex from "vuex";
import { createLocalVue, mount } from "@vue/test-utils";

import AlertStore from "@bluemind/alert.store";
import inject from "@bluemind/inject";

import MailAppStore from "../src/store";
import { messageKey as generateKey } from "../src/model/message";

const localVue = createLocalVue();
localVue.use(Vuex);

const folderUid = "folder:uid";
export const messageKey = generateKey(1, folderUid);
const userId = "6793466E-F5D4-490F-97BF-DF09D3327BF4";

inject.register({
    provide: "UserSession",
    use: { userId }
});

inject.register({
    provide: "i18n",
    use: {
        t: () => {},
        tc: () => {}
    }
});

const mailbox = {
    key: "MY_MAIBOX",
    type: "users",
    owner: userId
};

export function createStore() {
    const store = new Vuex.Store();
    store.registerModule("alert", AlertStore);
    store.registerModule("mail", MailAppStore);

    store.commit("mail/SET_MAX_MESSAGE_SIZE", 10);
    store.commit("mail/ADD_MAILBOXES", [mailbox]);
    store.commit("mail/SET_MAILBOX_FOLDERS", {
        folders: [{ key: folderUid, mailboxRef: { key: "MY_MAIBOX" } }],
        mailbox
    });
    store.commit("mail/ADD_MESSAGES", [{ key: messageKey, flags: [], folderRef: { key: folderUid, uid: folderUid } }]);
    store.commit("mail/SET_ACTIVE_MESSAGE", { key: messageKey });

    return store;
}

export function createWrapper(component, overrides, propsData = {}) {
    const defaultMountingOptions = {
        localVue,
        store: createStore(),
        propsData: propsData,
        mocks: {
            $t: () => {},
            $tc: () => {}
        }
    };
    const mergedMountingOptions = merge(defaultMountingOptions, overrides);
    return mount(component, mergedMountingOptions);
}
