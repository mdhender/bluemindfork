import inject from "@bluemind/inject";
import { mapExtensions } from "@bluemind/extensions";
import Command, { useCommand } from "./src/";
import { createLocalVue } from "@vue/test-utils";

jest.mock("@bluemind/extensions");
inject.register({ provide: "UserSession", factory: () => ({ roles: "myRole" }) });

self.bundleResolve = jest.fn().mockImplementation((id, callback) => callback());

let DummyVue;
describe("Command", () => {
    const localVue = createLocalVue();
    Command.install(localVue);
    beforeEach(() => {
        DummyVue = class {
            static mixin() {}
        };
    });
    test("Component has a $execute function", () => {
        expect(DummyVue.prototype.$execute).toBeFalsy();
        Command.install(DummyVue);
        expect(DummyVue.prototype.$execute).toBeTruthy();
    });

    test("commands defined in component are copied in the commandRegistry", async () => {
        const component = new localVue({
            commands: {
                myCommand: jest.fn()
            }
        });
        expect(Object.values(component.commandRegistry).length).toBe(1);
    });

    test("useCommand calls the registered function", async () => {
        const mockFn = jest.fn();
        const component = new localVue({
            setup() {
                return { execMyCommand: useCommand("myCommand", mockFn) };
            }
        });
        await component.execMyCommand();
        expect(component.commandRegistry.myCommand).toHaveBeenCalled();
    });

    test("Components share same reference of registry", async () => {
        const component = new localVue({
            setup() {
                return { execMyCommand: useCommand("myCommand", jest.fn()) };
            }
        });
        const component1 = new localVue({ commands: { otherCommand: jest.fn() } });
        const component2 = new localVue({});
        expect(component.commandRegistry).toEqual(component2.commandRegistry);
        expect(component.commandRegistry).toEqual(component1.commandRegistry);
    });

    test("$execute calls a command with given attributes", async () => {
        const component = new localVue();
        component.commandRegistry.myCommand = jest.fn();
        await component.$execute("my-command", "arg", { option1: 1 });
        expect(component.commandRegistry.myCommand).toHaveBeenCalledWith("arg", { option1: 1 });
    });
});

let component;
describe("Command with extensions", () => {
    const extensionOne = { fn: jest.fn(), name: "myCommand" };
    const extensionTwo = { fn: jest.fn(), name: "myCommand" };
    const localVue = createLocalVue();
    Command.install(localVue);
    beforeEach(() => {
        component = new localVue();
        component.commandRegistry.myCommand = jest.fn();

        mapExtensions.mockReset();
    });

    test("$execute calls several extension functions", async () => {
        mapExtensions.mockReturnValue({ command: [extensionOne, extensionTwo] });
        await component.$execute("my-command");
        expect(component.commandRegistry.myCommand).toHaveBeenCalled();
        expect(extensionOne.fn).toHaveBeenCalled();
        expect(extensionTwo.fn).toHaveBeenCalled();
    });

    test("$execute does not call extension with unowned role", async () => {
        const extensionOwnRole = { fn: jest.fn(), name: "myCommand", role: "myRole" };
        const extensionUnownedRole = { fn: jest.fn(), name: "myCommand", role: "neededRole" };
        const extensionNoRole = { fn: jest.fn(), name: "myCommand" };
        mapExtensions.mockReturnValue({ command: [extensionOwnRole, extensionUnownedRole, extensionNoRole] });
        await component.$execute("my-command");

        expect(component.commandRegistry.myCommand).toHaveBeenCalled();
        expect(extensionOwnRole.fn).toHaveBeenCalled();
        expect(extensionUnownedRole.fn).not.toHaveBeenCalled();
        expect(extensionNoRole.fn).toHaveBeenCalled();
    });

    test("Execution stops when StopExecution is thrown", async () => {
        const extensionWithStop = {
            fn: jest.fn().mockImplementation(() => {
                const error = new Error();
                error.name = "StopExecution";
                throw error;
            }),
            name: "myCommand"
        };

        mapExtensions.mockReturnValue({ command: [extensionOne, extensionWithStop] });
        await component.$execute("my-command");
        expect(extensionOne.fn).toHaveBeenCalled();
        expect(extensionWithStop.fn).toHaveBeenCalled();
        expect(component.commandRegistry.myCommand).not.toHaveBeenCalled();
    });

    test("Execution does not stop for other Error types", async () => {
        const extensionWithOtherError = {
            fn: jest.fn().mockImplementation(() => {
                const error = new Error();
                error.name = "OtherError";
                throw error;
            }),
            name: "myCommand"
        };

        mapExtensions.mockReturnValue({ command: [extensionOne, extensionWithOtherError] });
        await component.$execute("my-command");
        expect(extensionOne.fn).toHaveBeenCalled();
        expect(extensionWithOtherError.fn).toHaveBeenCalled();
        expect(component.commandRegistry.myCommand).toHaveBeenCalled();
    });

    test("Extension function with 'after' property is called after the component command", async () => {
        component.commandRegistry.myCommand = jest.fn().mockImplementation(() => {
            const error = new Error();
            error.name = "StopExecution";
            throw error;
        });

        const extensionBefore = {
            fn: jest.fn(),
            name: "myCommand",
            after: false
        };

        const extensionAfter = {
            fn: jest.fn(),
            name: "myCommand",
            after: true
        };

        mapExtensions.mockReturnValue({ command: [extensionAfter, extensionBefore] });
        await component.$execute("my-command");
        expect(extensionBefore.fn).toHaveBeenCalled();
        expect(component.commandRegistry.myCommand).toHaveBeenCalled();
        expect(extensionAfter.fn).not.toHaveBeenCalled();
    });
});
