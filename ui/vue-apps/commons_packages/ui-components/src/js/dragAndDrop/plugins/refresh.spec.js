import { MockInteractable } from "./Interactable.mock";
import { refresh } from "./refresh";
import debounce from "lodash.debounce";

jest.mock("lodash.debounce");
debounce.mockImplementation(fn => fn);

const scope = {
    Interactable: MockInteractable,
    interactStatic: { dynamicDrop: jest.fn() }
};

describe("Drag And Drop refresh plugin", () => {
    beforeEach(() => {
        scope.interactStatic.dynamicDrop.mockClear();
        debounce.mockClear();
    });
    test("add a refresh method to interactable object", () => {
        refresh.install(scope);
        expect(typeof scope.Interactable.prototype.refresh).toBe("function");
    });
    test("calling refresh set dynamicDrop to true", () => {
        refresh.install(scope);
        const element = new scope.Interactable();
        element.refresh();
        expect(scope.interactStatic.dynamicDrop).toHaveBeenCalledWith(true);
    });
    test("dynamicDrop is set to false after move", () => {
        refresh.install(scope);
        const element = new MockInteractable();
        element.refresh();
        element.fire("dragmove");
        expect(scope.interactStatic.dynamicDrop).toHaveBeenCalledWith(false);
    });
    test("dynamicDrop is set to false after dragend", () => {
        refresh.install(scope);
        const element = new MockInteractable();
        element.refresh();
        element.fire("dragend");
        expect(scope.interactStatic.dynamicDrop).toHaveBeenCalledWith(false);
    });
    test("autoscroll call refresh", () => {
        const element = new MockInteractable();
        element.refresh = jest.fn();
        refresh.listeners["interactable:new"]({ interactable: element });
        element.fire("autoscroll");
        expect(element.refresh).toHaveBeenCalled();
        expect(debounce).toHaveBeenCalled();
    });
});
