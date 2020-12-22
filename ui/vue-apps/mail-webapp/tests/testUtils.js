import merge from "lodash.merge";
import Vuex from "vuex";
import AlertStore from "@bluemind/alert.store";
import MailAppStore from "../src/store";
import OldMailAppStore from "../src/store.deprecated";
import inject from "@bluemind/inject";

import { createLocalVue, mount } from "@vue/test-utils";
import { ItemUri } from "@bluemind/item-uri";

const localVue = createLocalVue();
localVue.use(Vuex);

const folderUid = "folder:uid";
export const messageKey = ItemUri.encode("message:id", folderUid);
const userId = "6793466E-F5D4-490F-97BF-DF09D3327BF4";

inject.register({
    provide: "UserSession",
    use: { userId }
});

export function createStore() {
    const store = new Vuex.Store();
    store.registerModule("alert", AlertStore);
    store.registerModule("mail", MailAppStore);
    store.registerModule("mail-webapp", OldMailAppStore);

    store.commit("mail-webapp/setMaxMessageSize", 10);
    store.commit("mail/ADD_MAILBOXES", [
        {
            key: "MY_MAIBOX",
            type: "users",
            owner: userId
        }
    ]);
    store.commit("mail/SET_MAILBOX_FOLDERS", [{ key: folderUid, mailboxRef: { key: "MY_MAIBOX" } }]);
    store.commit("mail/ADD_MESSAGES", [{ key: messageKey, flags: [], folderRef: { key: folderUid, uid: folderUid } }]);
    store.commit("mail-webapp/currentMessage/update", { key: messageKey });

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
