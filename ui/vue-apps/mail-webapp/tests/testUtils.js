import cloneDeep from "lodash.clonedeep";
import merge from "lodash.merge";
import Vuex from "vuex";

import { createLocalVue, mount } from "@vue/test-utils";
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
                    maxMessageSize: 10
                },
                modules: {
                    currentMessage: {
                        namespaced: true,
                        state: {
                            key: messageKey
                        }
                    }
                }
            },
            mail: {
                namespaced: true,
                state: {
                    folders: {
                        [folderUid]: {
                            key: folderUid
                        }
                    },
                    messages: {
                        [messageKey]: { flags: [] }
                    },
                    selection: []
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
