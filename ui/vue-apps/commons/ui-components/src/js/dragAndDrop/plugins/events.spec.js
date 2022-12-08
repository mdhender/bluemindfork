import { MockInteractable } from "./Interactable.mock";
import { events } from "./events";

describe("Drag And Drop events plugin", () => {
    test("dispatch d&d events on dom element", () => {
        const element = new MockInteractable();
        events.listeners["interactable:new"]({ interactable: element });
        const draggable = createTarget();
        draggable.data = Math.random();
        const dropzone = createTarget();
        const interactable = createTarget();
        interactable.data = Math.random();
        const interaction = {};
        let event = { interactable, interaction };
        element.fire("dragstart", [event]);
        expect(interactable.target.dispatchEvent).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "dragstart",
                interactable,
                data: interactable.data,
                relatedInteractable: undefined
            })
        );
        interactable.target.dispatchEvent.mockClear();
        element.fire("dragend", [event]);
        expect(interactable.target.dispatchEvent).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "dragend",
                interactable,
                data: interactable.data,
                relatedInteractable: undefined
            })
        );
        interactable.target.dispatchEvent.mockClear();
        element.fire("dragmove", [event]);
        expect(interactable.target.dispatchEvent).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "drag",
                interactable,
                data: interactable.data,
                relatedInteractable: undefined
            })
        );
        interactable.target.dispatchEvent.mockClear();

        event = { dropzone, draggable, interaction };
        element.fire("dragover", [event]);
        expect(dropzone.target.dispatchEvent).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "dragover",
                interactable: dropzone,
                relatedData: draggable.data,
                relatedInteractable: draggable
            })
        );
        expect(draggable.target.dispatchEvent).not.toHaveBeenCalled();
        draggable.target.dispatchEvent.mockClear();
        dropzone.target.dispatchEvent.mockClear();

        element.fire("dragenter", [event]);
        expect(dropzone.target.dispatchEvent).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "dragenter",
                interactable: dropzone,
                relatedData: draggable.data,
                relatedInteractable: draggable
            })
        );
        expect(draggable.target.dispatchEvent).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "dragenter",
                interactable: draggable,
                data: draggable.data,
                relatedInteractable: dropzone
            })
        );
        draggable.target.dispatchEvent.mockClear();
        dropzone.target.dispatchEvent.mockClear();

        element.fire("dragleave", [event]);
        expect(dropzone.target.dispatchEvent).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "dragleave",
                interactable: dropzone,
                relatedData: draggable.data,
                relatedInteractable: draggable
            })
        );
        expect(draggable.target.dispatchEvent).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "dragleave",
                interactable: draggable,
                data: draggable.data,
                relatedInteractable: dropzone
            })
        );
        draggable.target.dispatchEvent.mockClear();
        dropzone.target.dispatchEvent.mockClear();

        element.fire("drop", [event]);
        expect(dropzone.target.dispatchEvent).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "drop",
                interactable: dropzone,
                relatedData: draggable.data,
                relatedInteractable: draggable
            })
        );
        expect(draggable.target.dispatchEvent).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "drop",
                interactable: draggable,
                data: draggable.data,
                relatedInteractable: dropzone
            })
        );
        draggable.target.dispatchEvent.mockClear();
        dropzone.target.dispatchEvent.mockClear();

        element.fire("holdover", [event]);
        expect(dropzone.target.dispatchEvent).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "holdover",
                interactable: dropzone,
                relatedData: draggable.data,
                relatedInteractable: draggable
            })
        );
        expect(draggable.target.dispatchEvent).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "holdover",
                interactable: draggable,
                data: draggable.data,
                relatedInteractable: dropzone
            })
        );
        draggable.target.dispatchEvent.mockClear();
        dropzone.target.dispatchEvent.mockClear();
    });
});

function createTarget() {
    const target = {
        target: {
            dispatchEvent: jest.fn()
        }
    };
    return target;
}
