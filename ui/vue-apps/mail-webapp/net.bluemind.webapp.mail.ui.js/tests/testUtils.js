import merge from "lodash.merge";
import Vuex from "vuex";
import { mount, createLocalVue } from "@vue/test-utils";
import cloneDeep from "lodash.clonedeep";
import MessageStore from "@bluemind/webapp.mail.store/src/MessageStore";

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
                    currentMessage: jest.fn(() => {
                        return { key: "", states: [] };
                    }),
                    my: jest.fn(() => ({})),
                    currentMessageAttachments: jest.fn(() => [{ mime: "" }]),
                    draft: jest.fn(() => {})
                },
                modules: {
                    currentMessage: cloneDeep(MessageStore),
                    draft: cloneDeep(MessageStore)
                },
                actions: {}
            }
        }
    };
    const mergedStoreOptions = merge(storeOptions, overrides);
    return new Vuex.Store(mergedStoreOptions);
}

export function createWrapper(component, overrides, propsData = {}) {
    console.log("propsData : ", propsData);
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
    console.log("propsData : ", mergedMountingOptions.propsData);
    return mount(component, mergedMountingOptions);
}
