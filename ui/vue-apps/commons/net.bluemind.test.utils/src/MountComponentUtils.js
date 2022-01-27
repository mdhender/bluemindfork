import Vuex from "vuex";

import router from "@bluemind/router";
import { mount, createLocalVue } from "@vue/test-utils";

const localVue = createLocalVue();
localVue.use(Vuex);

export default {
    createWrapper(component, store, propsData = {}) {
        const defaultMountingOptions = {
            localVue,
            store: new Vuex.Store(store),
            propsData,
            router,
            mocks: {
                $i18n: {
                    t: () => {},
                    tc: () => {}
                },
                $t: () => {},
                $tc: () => {}
            }
        };
        return mount(component, defaultMountingOptions);
    },

    mockSessionStore() {
        return {
            state: {},
            getters: {
                "preferences/SECTIONS": () => [
                    { code: "main", href: "/main/", icon: "wrench", categories: [{ code: "main" }] }
                ]
            },
            modules: {
                session: {
                    namespaced: true,
                    state: { settings: { remote: {}, local: {} } },
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
                        SET_SELECTED_SECTION: jest.fn()
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
