import { mount, createLocalVue } from "@vue/test-utils";

import Vuex from "vuex";
import router from "@bluemind/router";

const localVue = createLocalVue();
localVue.use(Vuex);

export default {
    createWrapper(component, store, propsData = {}) {
        const defaultMountingOptions = {
            router,
            store: new Vuex.Store(store),
            propsData,
            localVue,
            mocks: {
                $i18n: {
                    t: () => {},
                    tc: () => {},
                    te: () => {},
                    d: () => {}
                },
                $t: () => {},
                $tc: () => {},
                $te: () => {},
                $d: () => {}
            }
        };
        return mount(component, defaultMountingOptions);
    },

    mockSettingsStore() {
        return {
            state: {},
            getters: {
                "preferences/SECTIONS": () => [
                    { code: "main", href: "/main/", icon: "tool", categories: [{ code: "main" }] }
                ]
            },
            modules: {
                settings: {
                    namespaced: true,
                    state: {},
                    actions: { FETCH_ALL_SETTINGS: jest.fn() },
                    mutations: { SET_SETTINGS: jest.fn() }
                },
                preferences: {
                    namespaced: true,
                    state: {
                        offset: 0,
                        showPreferences: false,
                        selectedSectionCode: "mail",
                        sectionByCode: { main: {} }
                    },
                    mutations: {
                        SET_OFFSET: jest.fn(),
                        TOGGLE_PREFERENCES: jest.fn(),
                        SET_SECTIONS: jest.fn(),
                        SET_CURRENT_PATH: jest.fn()
                    },
                    modules: {
                        fields: {
                            namespaced: true,
                            state: {}
                        }
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
