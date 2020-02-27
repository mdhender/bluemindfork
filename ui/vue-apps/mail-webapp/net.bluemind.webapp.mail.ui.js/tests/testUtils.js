import merge from "lodash.merge";
import Vuex from "vuex";
import { mount, createLocalVue } from "@vue/test-utils";
import cloneDeep from "lodash.clonedeep";
import MessageStore from "@bluemind/webapp.mail.store/src/MessageStore";
import MailboxItemsStore from "@bluemind/backend.mail.store/src/MailboxItemsStore";

export function createStore(overrides) {
    const storeOptions = {
        state: {},
        modules: {
            "mail-webapp": {
                namespaced: true,
                state: {
                    currentFolderKey: "",
                    maxMessageSize: 10
                },
                getters: {
                    my: jest.fn(() => ({})),
                    currentMessageAttachments: jest.fn(() => [{ mime: "" }]),
                    draft: jest.fn(() => {})
                },
                modules: {
                    currentMessage: {
                        namespaced: true,
                        getters: {
                            message: jest.fn(() => {
                                return { key: "", states: [] };
                            })
                        }
                    },
                    draft: cloneDeep(MessageStore),
                    messages: cloneDeep(MailboxItemsStore)
                },
                actions: {}
            }
        }
    };
    const mergedStoreOptions = merge(storeOptions, overrides);
    return new Vuex.Store(mergedStoreOptions);
}

export function createWrapper(component, overrides, propsData = {}) {
    const localVue = createLocalVue();
    localVue.use(Vuex);
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
