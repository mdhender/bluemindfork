import { Actions, scheduleAction, cancelSchedulerActions } from "../../actions/draftActionsScheduler";
jest.useFakeTimers();
describe("Draft actions scheduler", () => {
    test("Execute SET_CONTENT, SAVE and SEND in order", async () => {
        let variable = "ZERO";
        const setContentCallBack = jest.fn(() => wait(100, () => (variable = "ONE")));
        scheduleAction(setContentCallBack, Actions.SET_CONTENT);
        const saveCallback = jest.fn(() => wait(50, () => (variable = "TWO")));
        scheduleAction(saveCallback, Actions.SAVE);
        const sendCallback = jest.fn(() => wait(1, () => (variable = "THREE")));
        scheduleAction(sendCallback, Actions.SEND);

        await jest.runAllTimersAsync();

        expect(setContentCallBack).toHaveBeenCalled();
        expect(saveCallback).toHaveBeenCalled();
        expect(sendCallback).toHaveBeenCalled();
        expect(variable).toBe("THREE");
    });

    test("When several calls in a row, it only calls the last callback", async () => {
        const setContentCallBack = jest.fn(() => wait(2));
        scheduleAction(setContentCallBack, Actions.SET_CONTENT);
        const setContentCallBack2 = jest.fn(() => wait(50));
        scheduleAction(setContentCallBack2, Actions.SET_CONTENT);
        await jest.runAllTimersAsync();

        expect(setContentCallBack).not.toHaveBeenCalled();
        expect(setContentCallBack2).toHaveBeenCalled();
    });
    test("scheduleAction with immediate=true argument does not wait for debounce", async () => {
        const setContentCallBack = jest.fn();

        const startImmediate = performance.now();
        await scheduleAction(setContentCallBack, Actions.SEND, true);
        const endImmediate = performance.now();

        const startNotImmediate = performance.now();
        scheduleAction(setContentCallBack, Actions.SEND);
        await jest.runAllTimersAsync();
        const endNotImmediate = performance.now();

        expect(endNotImmediate - startNotImmediate).toBeGreaterThan(endImmediate - startImmediate);
    });
    test("cancelSchedulerActions cancels the execution stack in waiting room", async () => {
        const setContentCallBack = jest.fn(() => wait(50));
        scheduleAction(setContentCallBack, Actions.SET_CONTENT);
        cancelSchedulerActions();
        await jest.runAllTimersAsync();

        expect(setContentCallBack).not.toHaveBeenCalled();
    });
});

function wait(duration, callback) {
    return new Promise(resolve => {
        setTimeout(() => {
            callback?.();
            resolve();
        }, duration);
    });
}
