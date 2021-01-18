import Vuex from "vuex";
import { mount, createLocalVue } from "@vue/test-utils";

const localVue = createLocalVue();
localVue.use(Vuex);

export default {
    createWrapper(component, store, propsData = {}) {
        const defaultMountingOptions = {
            localVue,
            store: new Vuex.Store(store),
            propsData,
            mocks: {
                $t: () => {},
                $tc: () => {}
            }
        };
        return mount(component, defaultMountingOptions);
    },

    mockSessionStore() {
        return {
            state: {},
            modules: {
                session: {
                    namespaced: true,
                    state: {
                        userSettings: {}
                    },
                    actions: {
                        FETCH_ALL_SETTINGS: jest.fn(),
                        UPDATE_ALL_SETTINGS: jest.fn()
                    }
                },
                "root-app": {
                    namespaced: true,
                    state: {
                        quota: {}
                    }
                }
            }
        };
    }
};
