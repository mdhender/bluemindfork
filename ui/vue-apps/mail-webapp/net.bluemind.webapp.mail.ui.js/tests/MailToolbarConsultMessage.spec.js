import { mount, createLocalVue } from "@vue/test-utils";
import merge from "lodash.merge";
import Vuex from "vuex";
import MailToolbarConsultMessage from "../src/MailToolbar/MailToolbarConsultMessage";
jest.mock("@bluemind/styleguide/css/_variables.scss", () => ({ iconsColors: "" }));

function createStore(overrides) {
    const storeOptions = {
        state: {},
        modules: {
            "mail-webapp": {
                namespaced: true,
                state: { currentFolderKey: "" },
                getters: {
                    my: jest.fn(() => {
                        return {};
                    })
                },
                actions: {}
            },
            "mail-webapp/currentMessage": {
                namespaced: true,
                state() {
                    return {
                        id: undefined,
                        key: undefined,
                        parts: { attachments: [], inlines: [] },
                        saveDate: null,
                        status: null
                    };
                },
                getters: {
                    message: jest.fn(() => {
                        return { key: "", states: [] };
                    })
                }
            }
        }
    };
    const mergedStoreOptions = merge(storeOptions, overrides);
    return new Vuex.Store(mergedStoreOptions);
}

function createWrapper(overrides) {
    const localVue = createLocalVue();
    localVue.use(Vuex);
    const defaultMountingOptions = {
        localVue,
        store: createStore(),
        propsData: {},
        mocks: {
            $t: () => {},
            $tc: () => {}
        }
    };
    const mergedMountingOptions = merge(defaultMountingOptions, overrides);
    return mount(MailToolbarConsultMessage, mergedMountingOptions);
}

describe("MailToolbarConsultMessage", () => {
    test("is a Vue instance", () => {
        const wrapper = createWrapper();
        expect(wrapper.isVueInstance()).toBeTruthy();
    });

    test("should match snapshot", () => {
        const wrapper = createWrapper();
        expect(wrapper.element).toMatchSnapshot();
    });

    test("should display 'mark unread' button if the message is read", () => {
        const wrapper = createWrapper();
        expect(wrapper.contains(".btn.read")).toBeTruthy();
        expect(!wrapper.contains(".btn.unread")).toBeTruthy();
    });

    test("should display 'mark read' button if the message is unread", () => {
        const storeWithReadMessage = createStore({
            modules: {
                "mail-webapp/currentMessage": {
                    getters: {
                        message: jest.fn(() => {
                            return { key: "", states: ["not-seen"] };
                        })
                    }
                }
            }
        });

        const wrapper = createWrapper({ store: storeWithReadMessage });
        expect(wrapper.contains(".btn.unread")).toBeTruthy();
        expect(!wrapper.contains(".btn.read")).toBeTruthy();
    });
});
