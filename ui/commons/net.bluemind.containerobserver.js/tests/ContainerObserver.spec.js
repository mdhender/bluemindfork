import WebSocketClient from "@bluemind/sockjs";
import injector from "@bluemind/inject";
import ContainerObserver from "../src";

jest.mock("@bluemind/sockjs");
jest.mock("@bluemind/inject");
const VueBus = {
    $emit: jest.fn()
};
injector.getProvider.mockReturnValue({
    get: () => VueBus
});

describe("ContainerObserver", () => {
    beforeEach(() => {
        VueBus.$emit.mockClear();
    });
    test("Observing a container send a registery request on websocket", () => {
        ContainerObserver.observe("container_type", "uid");
        const instance = WebSocketClient.mock.instances[0];
        expect(instance.register).toHaveBeenCalledWith("bm.container_type.hook.uid.changed", ContainerObserver.notify);
    });
    test("Forgetting a container send an unregistery request on websocket", () => {
        ContainerObserver.forget("container_type", "uid");
        const instance = WebSocketClient.mock.instances[0];
        expect(instance.unregister).toHaveBeenCalledWith("bm.container_type.hook.uid.changed");
    });
    test("When an observed container changes, a message is broadcasted on VueBus", () => {
        ContainerObserver.observe("container_type", "uid");
        ContainerObserver.notify({ data: { requestId: "bm.container_type.hook.uid.changed" } });
        expect(VueBus.$emit).toHaveBeenCalledWith("container_type_changed", { container: "uid" });
    });
});
