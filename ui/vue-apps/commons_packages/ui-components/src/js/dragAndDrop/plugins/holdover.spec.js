import { MockInteractable } from "./Interactable.mock";
import { holdover } from "./holdover";

const scope = {
    Interactable: MockInteractable
};
jest.useFakeTimers();

describe("Drag And Drop holdover plugin", () => {
    beforeAll(() => {
        scope.actions = { phaselessTypes: {} };
        holdover.install(scope);
    });
    test("fire holdover event after dragenter", () => {
        const element = new MockInteractable();
        holdover.listeners["interactable:new"]({ interactable: element });
        const event = { interaction: {} };

        element.fire("dragenter", [event]);
        element.fire = jest.fn();
        jest.runAllTimers();
        expect(element.fire).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "holdover"
            })
        );
    });
    test("not fire holdover event after dragenter and dragleave", () => {
        const element = new MockInteractable();
        holdover.listeners["interactable:new"]({ interactable: element });
        const event = { interaction: {} };

        element.fire("dragenter", [event]);
        element.fire("dragleave");
        element.fire = jest.fn();
        jest.advanceTimersByTime(800);
        expect(element.fire).not.toHaveBeenCalled();
    });
});
