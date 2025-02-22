import EventTarget from "../src/EventTarget";

describe("EventTarget", () => {
    test("A listener is called when the associated event is dispatched until the listener is removed", done => {
        const target = new EventTarget();
        const callback = jest.fn();
        target.addEventListener("any", callback);
        target.dispatchEvent(new Event("any"));
        target.dispatchEvent(new Event("any"));
        target.removeEventListener("any", callback);
        target.dispatchEvent(new Event("any"));
        target.addEventListener("end", () => {
            expect(callback).toHaveBeenCalledTimes(2);
            done();
        });
        target.dispatchEvent(new Event("end"));
    });
    test("A listener is called with the 'once' option is called only.. once", done => {
        const target = new EventTarget();
        const callback = jest.fn();
        target.addEventListener("any", callback, { once: true });
        target.dispatchEvent(new Event("any"));
        target.dispatchEvent(new Event("any"));
        target.dispatchEvent(new Event("any"));
        target.addEventListener("end", () => {
            expect(callback).toHaveBeenCalledTimes(1);
            done();
        });
        target.dispatchEvent(new Event("end"));
    });
    test("A removed listener will not be called when event is dispatched.", done => {
        const target = new EventTarget();
        const callback = jest.fn();
        const another = jest.fn();
        target.addEventListener("any", callback);
        target.addEventListener("any", another);
        target.removeEventListener("any", callback);

        target.dispatchEvent(new Event("any"));

        target.addEventListener("end", () => {
            expect(callback).toHaveBeenCalledTimes(0);
            expect(another).toHaveBeenCalledTimes(1);
            done();
        });
        target.dispatchEvent(new Event("end"));
    });
    test("Removing a type of event should remove all listeners.", done => {
        const target = new EventTarget();
        const callback = jest.fn();
        const another = jest.fn();
        target.addEventListener("any", callback);
        target.addEventListener("any", another);
        target.removeEventListener("any");

        target.dispatchEvent(new Event("any"));

        target.addEventListener("end", () => {
            expect(callback).toHaveBeenCalledTimes(0);
            expect(another).toHaveBeenCalledTimes(0);
            done();
        });
        target.dispatchEvent(new Event("end"));
    });
    test("Event can be re-add and re-removed.", done => {
        const target = new EventTarget();
        const callback = jest.fn();
        const another = jest.fn();
        target.addEventListener("any", callback);
        target.addEventListener("any", another);
        target.dispatchEvent(new Event("any"));

        target.removeEventListener("any", callback);
        target.dispatchEvent(new Event("any"));

        target.addEventListener("any", callback);
        target.dispatchEvent(new Event("any"));

        target.removeEventListener("any");
        target.dispatchEvent(new Event("any"));

        target.addEventListener("end", () => {
            expect(callback).toHaveBeenCalledTimes(2);
            expect(another).toHaveBeenCalledTimes(3);
            done();
        });
        target.dispatchEvent(new Event("end"));
    });
    test("Clear removes all listeners.", done => {
        const target = new EventTarget();
        const another = jest.fn();
        const one = jest.fn();
        const bite = jest.fn();

        target.addEventListener("another", another);
        target.addEventListener("one", one);
        target.addEventListener("bite", bite);
        target.dispatchEvent(new Event("another"));
        target.dispatchEvent(new Event("one"));
        target.dispatchEvent(new Event("bite"));

        target.clear();
        target.dispatchEvent(new Event("another"));
        target.dispatchEvent(new Event("one"));
        target.dispatchEvent(new Event("bite"));

        target.addEventListener("end", () => {
            expect(another).toHaveBeenCalledTimes(1);
            expect(one).toHaveBeenCalledTimes(1);
            expect(bite).toHaveBeenCalledTimes(1);
            done();
        });
        target.dispatchEvent(new Event("end"));
    });

    test("Dispatch can be called with a string or native event.", done => {
        const target = new EventTarget();
        const callback = jest.fn();
        target.addEventListener("any", callback);
        target.dispatchEvent("any");
        target.dispatchEvent(new Event("any"));
        target.addEventListener("end", () => {
            expect(callback).toHaveBeenCalledTimes(2);
            done();
        });
        target.dispatchEvent(new Event("end"));
    });
    test("has return true if EventTarget listen to this event type .", () => {
        const target = new EventTarget();
        const callback = jest.fn();
        const callback2 = jest.fn();
        target.addEventListener("any", callback);
        target.addEventListener("any", callback2);
        expect(target.has("any")).toBeTruthy();
        expect(target.has("any", callback)).toBeTruthy();
        target.removeEventListener("any", callback);
        expect(target.has("any")).toBeTruthy();
    });
    test("has return false if EventTarget do not have a listener for this event type .", () => {
        const target = new EventTarget();
        const callback = jest.fn();
        const callback2 = jest.fn();
        const callback3 = jest.fn();
        target.addEventListener("any", callback);
        target.addEventListener("any", callback2);
        expect(target.has("none")).toBeFalsy();
        expect(target.has("any", callback3)).toBeFalsy();
        target.removeEventListener("any", callback);
        expect(target.has("any", callback)).toBeFalsy();
        target.removeEventListener("any", callback2);
        expect(target.has("any")).toBeFalsy();
    });
});
