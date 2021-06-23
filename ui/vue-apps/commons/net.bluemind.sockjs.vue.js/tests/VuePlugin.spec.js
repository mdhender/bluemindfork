import VueSockJSPlugin from "../src/index.js";
import { createLocalVue, shallowMount } from "@vue/test-utils";
import WebSocketClient from "@bluemind/sockjs";

jest.mock("@bluemind/sockjs");

const emit = jest.fn();
const VueBus = {
    Client() {
        this.$emit = emit;
    }
};
let listeners = {};
const addEventListener = jest.fn().mockImplementation((type, listener) => {
    listeners[type] = listener;
});

WebSocketClient.use.mockImplementation(fn => fn({ addEventListener }));
describe("SockjsVuePlugin", () => {
    let TestComponent;
    let Vue;
    beforeEach(() => {
        TestComponent = {
            template: "<div><slot /></div>",
            bus: {
                busEvent: jest.fn()
            }
        };
        emit.mockClear();
        listeners = {};
        Vue = createLocalVue();
        Vue.use(VueSockJSPlugin, VueBus);
    });
    test("A WebSocketClient is installed in Vue instance", () => {
        const wrapper = shallowMount(TestComponent, {
            localVue: Vue
        });
        expect(wrapper.vm.$socket).toBeDefined();
    });

    test("On auth error, a disconnected message is sent on the bus", () => {
        expect(listeners["response"]).toBeDefined();
        listeners["response"]({ data: { statusCode: 401 } });
        expect(emit).toHaveBeenCalledWith("disconnected");
    });

    test("Other response does not trigger a disconnected message", () => {
        expect(listeners["response"]).toBeDefined();
        listeners["response"]({ data: { statusCode: 200 } });
        listeners["response"]({ data: { statusCode: 500 } });
        listeners["response"]({ data: { statusCode: 404 } });
        expect(emit).not.toHaveBeenCalledWith("disconnected");
    });
    test("On online switch, an online message is sent on the bus", () => {
        expect(listeners["online"]).toBeDefined();
        listeners["online"]({ online: true });
        expect(emit).toHaveBeenCalledWith("online", true);
        listeners["online"]({ online: false });
        expect(emit).toHaveBeenLastCalledWith("online", false);
    });
});
