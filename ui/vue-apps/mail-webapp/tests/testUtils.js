import merge from "lodash.merge";
import Vuex from "vuex";
import { mount, createLocalVue } from "@vue/test-utils";
import cloneDeep from "lodash.clonedeep";
import MessageStore from "../src/store.deprecated/MessageStore";
import MailboxItemsStore from "../src/store.deprecated/mailbackend/MailboxItemsStore";

export function createStore(overrides) {
    const storeOptions = {
        state: {},
        modules: {
            "mail-webapp": {
                namespaced: true,
                state: {
                    maxMessageSize: 10,
                    selectedMessageKeys: []
                },
                getters: {
                    my: jest.fn(() => ({})),
                    currentMessageAttachments: jest.fn(() => [{ mime: "" }]),
                    draft: jest.fn(() => {}),
                    matchingFolders: jest.fn(() => () => [])
                },
                modules: {
                    currentMessage: {
                        namespaced: true,
                        getters: {
                            message: jest.fn(() => {
                                return {
                                    key:
                                        "WyIxNWUwZjNjYS01M2E2LTRiYmItYWQ0NS02MTgwNjcyYmE4ZWMiLCIzNUU1MTJCOC0xRDVBLTRENkQtQUMzOC01QzY4OENDQzlBMDUiXQ==",
                                    states: [],
                                    flags: []
                                };
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
