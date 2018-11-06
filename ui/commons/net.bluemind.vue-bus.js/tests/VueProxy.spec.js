import VueProxy from "../src/VueProxy";
import { createLocalVue } from "@vue/test-utils";
import { shallowMount } from "@vue/test-utils";

const Vue = createLocalVue();
const FakeBus = { Client: Vue };
Vue.use(VueProxy, FakeBus);
const $bus = new Vue();

describe("VueProxy", () => {
    let TestComponent;
    beforeEach(() => {
        TestComponent = {
            template: "<div><slot /></div>",
            bus: {
                busEvent: jest.fn()
            }
        };
    }),
    test("Event bus is injected in all Components", () => {
        const wrapper = shallowMount(TestComponent, {
            localVue: Vue
        });

        expect(wrapper.vm.$bus).toBeDefined();
    });
    test("Component.bus.myEvent callback is called when $bus.emit('myEvent'); ", () => {
        shallowMount(TestComponent, {
            localVue: Vue,
            mocks: {
                $bus
            }
        });

        $bus.$emit("busEvent", "busEvent", {});
        expect(TestComponent.bus.busEvent).toBeCalled();
        $bus.$emit("busEvent", "busEvent", {});
        expect(TestComponent.bus.busEvent).toBeCalledTimes(2);
    }),
    test("Component.bus.myEvent callback is called with event payload ", () => {
        shallowMount(TestComponent, {
            localVue: Vue,
            mocks: {
                $bus
            }
        });
        const payload = { dummy: true };
        $bus.$emit("busEvent", "busEvent", payload);
        expect(TestComponent.bus.busEvent).toBeCalledWith(payload);
    }),
    test("Component.bus.myEvent callback is only called when $bus.emit('myEvent');", () => {
        shallowMount(TestComponent, {
            localVue: Vue,
            mocks: {
                $bus
            }
        });
        $bus.$emit("anotherBusEvent", "anotherBusEvent", {});
        expect(TestComponent.bus.busEvent).not.toBeCalled();
    });
    test("Callback will not be called after component lifecycle end;", () => {
        const wrapper = shallowMount(TestComponent, {
            localVue: Vue,
            mocks: {
                $bus
            }
        });
        $bus.$emit("busEvent", "busEvent", {});
        expect(TestComponent.bus.busEvent).toBeCalled();
        wrapper.destroy();
        $bus.$emit("busEvent", "busEvent", {});
        expect(TestComponent.bus.busEvent).toBeCalledTimes(1);
    });
});
