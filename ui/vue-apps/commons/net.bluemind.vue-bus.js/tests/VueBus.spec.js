import VueBus from "../src/index.js";
import { createLocalVue } from "@vue/test-utils";
import { shallowMount } from "@vue/test-utils";
import Vuex from "vuex";

const Vue = createLocalVue();
Vue.use(Vuex);
const mutations = { $_VueBus_dummy: jest.fn() };
const store = new Vuex.Store({ mutations });
Vue.use(VueBus, store);

describe("VueBus", () => {
    let TestComponent;
    beforeEach(() => {
        TestComponent = {
            template: "<div><slot /></div>",
            bus: {
                busEvent: jest.fn()
            }
        };
    }),
        test("VueProxy is installed", () => {
            const wrapper = shallowMount(TestComponent, {
                localVue: Vue
            });

            expect(wrapper.vm.$bus).toBeDefined();
        });
    test("VuexProxy is started", () => {
        new VueBus.Client().$emit("dummy", {});
        expect(mutations.$_VueBus_dummy).toBeCalled();
    });
    test("All instance of VueBus share the same bus", () => {
        const spy = jest.fn();
        new VueBus.Client().$on("dummy", spy);
        new VueBus.Client().$emit("dummy", {});

        expect(spy).toBeCalled();
    });
    test("Callback are called with event name and payload", () => {
        const spy = jest.fn();
        new VueBus.Client().$on("dummy", spy);
        const payload = { big: "up" };
        new VueBus.Client().$emit("dummy", payload);

        expect(spy).toBeCalledWith("dummy", payload);
    });
    test("Callback on * are called on all events", () => {
        const spy = jest.fn();
        new VueBus.Client().$on("*", spy);
        ["The", "spy", "method", "will", "be", "called", "8", "times"].forEach(event =>
            new VueBus.Client().$emit(event, {})
        );

        expect(spy).toBeCalledTimes(8);
    });
    test("Callback stop being called after using 'off'", () => {
        const spy = jest.fn();
        const aSpyHideAnother = jest.fn();
        new VueBus.Client().$on("*", spy);
        new VueBus.Client().$on("*", aSpyHideAnother);
        new VueBus.Client().$emit("dummy", {});
        new VueBus.Client().$off("*", spy);
        new VueBus.Client().$emit("dummy", {});

        expect(spy).toBeCalledTimes(1);
        expect(aSpyHideAnother).toBeCalledTimes(2);
    });
    test("Calling off with one parameter remove all function bind on the event", () => {
        const spy = jest.fn();
        const aSpyHideAnother = jest.fn();
        new VueBus.Client().$on("*", spy);
        new VueBus.Client().$on("*", aSpyHideAnother);
        new VueBus.Client().$emit("dummy", {});
        new VueBus.Client().$off("*");
        new VueBus.Client().$emit("dummy", {});

        expect(spy).toBeCalledTimes(1);
        expect(aSpyHideAnother).toBeCalledTimes(1);
    });
    test("Calling off without parameters remove all callbacks", () => {
        const multiPassSpy = jest.fn();
        const politicalSpy = jest.fn();
        const ninjaSpy = jest.fn();
        new VueBus.Client().$on("*", multiPassSpy);
        new VueBus.Client().$on("Trump", politicalSpy);
        new VueBus.Client().$on("samourai", ninjaSpy);
        new VueBus.Client().$emit("Trump", {});
        new VueBus.Client().$emit("samourai", {});
        new VueBus.Client().$off();
        new VueBus.Client().$emit("Trump", {});
        new VueBus.Client().$emit("samourai", {});
        expect(multiPassSpy).toBeCalledTimes(2);
        expect(politicalSpy).toBeCalledTimes(1);
        expect(ninjaSpy).toBeCalledTimes(1);
    });
});
