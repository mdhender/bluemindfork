import { MockInteractable } from "./Interactable.mock";
import { tooltip } from "./tooltip";
const scope = {
    Interactable: MockInteractable
};
describe("Drag And Drop tooltip plugin", () => {
    test("add a tooltip method to interactable object", () => {
        tooltip.install(scope);
        expect(typeof scope.Interactable.prototype.tooltip).toBe("function");
    });

    test("calling tooltip add a tooltip to the dragged object", () => {
        tooltip.install(scope);
        const element = new scope.Interactable();
        element.on = jest.fn();
        element.tooltip({ text: "My text" });
        expect(element.options.tooltip).toBeDefined();
        expect(element.options.tooltip.text).toEqual("My text");
        expect(element.options.tooltip.element.innerHTML).toContain("My text");
        // All the mocks needed to test the tooltip behaviour would make the
        // test irrelevant.
        expect(element.on).toHaveBeenCalledWith("dragstart", expect.anything());
        expect(element.on).toHaveBeenCalledWith("dragmove", expect.anything());
        expect(element.on).toHaveBeenCalledWith("dragend", expect.anything());
    });
});
