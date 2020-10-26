import VuexProxy from "../src/VuexProxy";
import { createLocalVue } from "@vue/test-utils";
import Vuex from "vuex";

const Vue = createLocalVue();
Vue.use(Vuex);

describe("VuexProxy", () => {
    let store;
    const bus = new Vue();
    beforeEach(() => {
        store = new Vuex.Store({
            mutations: {
                dummy: jest.fn()
            },
            actions: {
                dummy: jest.fn()
            }
        });
    });

    afterEach(() => {
        VuexProxy.stop();
    });

    test("Proxy execute mutations/actions named after the event type without considering non-word separators", () => {
        const spy = jest.fn();
        store.hotUpdate({
            mutations: {
                $_VueBus_THIS_WILLBE_CALLED_2_times: spy
            },
            actions: {
                $_VueBus_ThiswillBE_called2times: spy
            }
        });
        VuexProxy.start(bus, store);
        bus.$emit("*", "t.H-i s/w,i;l:l!b@e^c%a*l&l)e<d'2-t_i\\m{e^s", {});
        expect(spy).toHaveBeenCalledTimes(2);
    });

    test("Proxy execute mutation / actions with '$emit' arguments as payload", () => {
        const spy = jest.fn();
        store.hotUpdate({
            mutations: {
                $_VueBus_BUSPUBLISHTHIS: spy
            },
            actions: {
                $_VueBus_busAlsoPublishThis: spy
            }
        });
        VuexProxy.start(bus, store);
        const payload = { body: "changed" };
        bus.$emit("*", "buspublishthis", payload);
        expect(spy).toHaveBeenNthCalledWith(1, expect.anything(), payload);
        bus.$emit("*", "busAlsoPublishThis", payload);
        expect(spy).toHaveBeenNthCalledWith(2, expect.anything(), payload, undefined);
    });

    test("Mutations/Actions without prefix or not matching event id will not be called", () => {
        const spy = jest.fn();
        store.hotUpdate({
            mutations: {
                CALL_ME: spy
            },
            actions: {
                $_VueBus_CALL_ME_BABY: spy
            }
        });
        VuexProxy.start(bus, store);
        bus.$emit("*", "callMe", {});
        expect(spy).not.toBeCalled();
    });
});
