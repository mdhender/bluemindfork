import { MockInteractable } from "./Interactable.mock";
import { states } from "./states";
const scope = {
    Interactable: MockInteractable
};
describe("Drag And Drop states plugin", () => {
    test("add states methods to interactable object", () => {
        states.install(scope);
        expect(typeof scope.Interactable.prototype.addState).toBe("function");
        expect(typeof scope.Interactable.prototype.disableState).toBe("function");
    });

    test("add a states class on target ", () => {
        states.install(scope);
        const element = new scope.Interactable();
        states.listeners["interactable:new"]({ interactable: element });

        const dropzone = createTarget();

        let event = createTarget();
        event.interactable = createTarget;
        element.fire("dragstart", [event]);
        expect(event.target.classList.add).toHaveBeenCalledWith("bm-drag-active");
        element.fire("dragend", [event]);
        expect(event.target.classList.remove).toHaveBeenCalledWith("bm-drag-active");
        event.interactable = undefined;
        event.dropzone = dropzone;
        element.fire("dropactivate", [event]);
        expect(event.target.classList.add).toHaveBeenCalledWith("bm-dropzone-active");
        element.fire("dragenter", [event]);
        expect(event.target.classList.add).toHaveBeenCalledWith("bm-dropzone-hover");
        element.fire("dragleave", [event]);
        expect(event.target.classList.remove).toHaveBeenCalledWith("bm-dropzone-hover");
        element.fire("dropdeactivate", [event]);
        expect(event.target.classList.remove).toHaveBeenCalledWith("bm-dropzone-active");
        expect(event.target.classList.remove).toHaveBeenCalledWith("bm-dropzone-hover");
    });
    test("disableState state remove state from supported state", () => {
        states.install(scope);
        const element = new scope.Interactable();
        states.listeners["interactable:new"]({ interactable: element });

        expect(element.options.supportedStates).toContain("hover");
        expect(element.options.supportedStates).toContain("active");
        element.disableState("active");
        expect(element.options.supportedStates).toContain("hover");
        expect(element.options.supportedStates).not.toContain("active");
        let event = createTarget();
        element.fire("dragstart", [event]);
        expect(event.target.classList.add).not.toHaveBeenCalled();
    });
    test("addState let you define custom states", () => {
        states.install(scope);
        const element = new scope.Interactable();
        states.listeners["interactable:new"]({ interactable: element });

        const dropzone = createTarget();
        const interactable = createTarget();
        let event = createTarget();
        event.dropzone = dropzone;
        event.draggable = interactable;
        expect(element.options.supportedStates).not.toContain("dummy");
        element.addState("dummy", { start: ["dummyOn"], end: ["dummyOff"] });
        element.addState("related", { start: ["dummyOn"], end: ["dummyOff"], target: "related" });
        element.addState("original", { start: ["dummyOn"], end: ["dummyOff"], target: "original" });
        expect(element.options.supportedStates).toContain("dummy");
        expect(element.options.supportedStates).toContain("related");
        expect(element.options.supportedStates).toContain("original");
        element.fire("dummyOn", [event]);
        expect(event.target.classList.add).toHaveBeenCalledWith("bm-dropzone-dummy");
        expect(interactable.target.classList.add).toHaveBeenCalledWith("bm-dropzone-related");
        expect(dropzone.target.classList.add).toHaveBeenCalledWith("bm-dropzone-original");
        element.fire("dummyOff", [event]);
        expect(event.target.classList.remove).toHaveBeenCalledWith("bm-dropzone-dummy");
        expect(interactable.target.classList.remove).toHaveBeenCalledWith("bm-dropzone-related");
        expect(dropzone.target.classList.remove).toHaveBeenCalledWith("bm-dropzone-original");
    });
});

function createTarget() {
    const target = {
        target: {
            classList: { add: jest.fn(), remove: jest.fn() }
        }
    };
    return target;
}
