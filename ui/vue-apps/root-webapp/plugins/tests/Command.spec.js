import inject from "@bluemind/inject";
import { mapExtensions } from "@bluemind/extensions";
import Command from "../Command";

jest.mock("@bluemind/extensions");
inject.register({ provide: "UserSession", factory: () => ({ roles: "myRole" }) });

self.bundleResolve = jest.fn().mockImplementation((id, callback) => callback());

let DummyVue;
describe("Command", () => {
    beforeEach(() => {
        DummyVue = class {};
    });
    test("Component has a $execute function", () => {
        expect(DummyVue.prototype.$execute).toBeFalsy();
        Command.install(DummyVue);
        expect(DummyVue.prototype.$execute).toBeTruthy();
    });

    test("$execute calls a command with given attributes", async () => {
        DummyVue = class {
            constructor() {
                this.$options = {
                    commands: {
                        myCommand: jest.fn()
                    }
                };
            }
        };
        Command.install(DummyVue);
        const component = new DummyVue();
        await component.$execute("my-command", "arg");
        expect(component.$options.commands.myCommand).toHaveBeenCalledWith("arg");
    });
});

let component;
describe("Command with extensions", () => {
    const extensionOne = { fn: jest.fn(), name: "myCommand" };
    const extensionTwo = { fn: jest.fn(), name: "myCommand" };
    DummyVue = class {
        constructor() {
            this.$options = {
                commands: {
                    myCommand: jest.fn()
                }
            };
        }
    };
    Command.install(DummyVue);

    beforeEach(() => {
        component = new DummyVue();
        mapExtensions.mockReset();
    });

    test("$execute calls several extension functions", async () => {
        mapExtensions.mockReturnValue({ command: [extensionOne, extensionTwo] });
        await component.$execute("my-command");
        expect(component.$options.commands.myCommand).toHaveBeenCalled();
        expect(extensionOne.fn).toHaveBeenCalled();
        expect(extensionTwo.fn).toHaveBeenCalled();
    });

    test("$execute does not call extension with unowned role", async () => {
        const extensionOwnRole = { fn: jest.fn(), name: "myCommand", role: "myRole" };
        const extensionUnownedRole = { fn: jest.fn(), name: "myCommand", role: "neededRole" };
        const extensionNoRole = { fn: jest.fn(), name: "myCommand" };
        mapExtensions.mockReturnValue({ command: [extensionOwnRole, extensionUnownedRole, extensionNoRole] });
        await component.$execute("my-command");

        expect(component.$options.commands.myCommand).toHaveBeenCalled();
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
        expect(component.$options.commands.myCommand).not.toHaveBeenCalled();
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
        expect(component.$options.commands.myCommand).toHaveBeenCalled();
    });

    test("Extension function with 'after' property is called after the component command", async () => {
        DummyVue = class {
            constructor() {
                this.$options = {
                    commands: {
                        myCommand: jest.fn().mockImplementation(() => {
                            const error = new Error();
                            error.name = "StopExecution";
                            throw error;
                        })
                    }
                };
            }
        };
        Command.install(DummyVue);
        component = new DummyVue();

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
        expect(component.$options.commands.myCommand).toHaveBeenCalled();
        expect(extensionAfter.fn).not.toHaveBeenCalled();
    });
});
