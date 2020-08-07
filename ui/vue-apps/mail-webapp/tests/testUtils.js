import merge from "lodash.merge";
import Vuex from "vuex";
import { mount, createLocalVue } from "@vue/test-utils";
import cloneDeep from "lodash.clonedeep";
import { ItemUri } from "@bluemind/item-uri";
import MessageStore from "../src/store.deprecated/MessageStore";
import MailboxItemsStore from "../src/store.deprecated/mailbackend/MailboxItemsStore";

const localVue = createLocalVue();
localVue.use(Vuex);

const folderUid = "folder:uid";
export const messageKey = ItemUri.encode("message:id", folderUid);

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
                                    key: messageKey,
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
            },
            mail: {
                namespaced: true,
                state: {
                    folders: {
                        [folderUid]: {
                            key: folderUid
                        }
                    }
                },
                getters: {
                    MY_TRASH: () => {},
                    MY_INBOX: () => {}
                }
            }
        }
    };
    const mergedStoreOptions = merge(storeOptions, overrides);
    return new Vuex.Store(mergedStoreOptions);
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
