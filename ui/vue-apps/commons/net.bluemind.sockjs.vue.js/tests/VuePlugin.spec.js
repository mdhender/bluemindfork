import VueSockJSPlugin from "../src/index.js";
import { createLocalVue } from "@vue/test-utils";
import { shallowMount } from "@vue/test-utils";
import WebSocketClient from "@bluemind/sockjs";

jest.mock("@bluemind/sockjs");

const Vue = createLocalVue();
const emit = jest.fn();
const VueBus = {
    Client() {
        this.$emit = emit;
    }
};

const socket = {
    onOnline(listener) {
        listener({ type: "online", online: true });
    },
    ping(listener) {
        listener({ statusCode: 401 });
    },
    use() {}
};
WebSocketClient.mockImplementation(() => socket);
Vue.use(VueSockJSPlugin, VueBus);

describe("SockjsVuePlugin", () => {
    let TestComponent;
    beforeEach(() => {
        TestComponent = {
            template: "<div><slot /></div>",
            bus: {
                busEvent: jest.fn()
            }
        };
    }),
        test("A WebSocketClient is installed in Vue instance", () => {
            const wrapper = shallowMount(TestComponent, {
                localVue: Vue
            });
            expect(wrapper.vm.$socket).toBeDefined();
        });
    test("On ping error, a disconnected message is sent on the bus", () => {
        expect(emit).toHaveBeenNthCalledWith(2, "disconnected");
    });
    test("On online switch, an online message is sent on the bus", () => {
        expect(emit).toHaveBeenNthCalledWith(1, "online", true);
    });
});
