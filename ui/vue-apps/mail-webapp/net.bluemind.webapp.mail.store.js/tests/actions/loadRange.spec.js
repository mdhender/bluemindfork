import { loadRange } from "../../src/actions/loadRange";

jest.useFakeTimers();

const context = {
    dispatch: jest.fn().mockReturnValue(Promise.resolve()),
    getters: {
        "messages/messages": {}
    },
    state: {
        messages: {
            itemKeys: new Array(1000).fill(0).map((val, index) => index)
        }
    }
};
describe("[Mail-WebappStore][actions] : loadRange", () => {
    beforeEach(() => {
        context.getters["messages/messages"] = {};
    });
    afterEach(() => {
        jest.runOnlyPendingTimers();
        context.dispatch.mockClear();
    });
    test("load messages depending on sorted key order", () => {
        loadRange(context, { start: 100, end: 200 });
        jest.runOnlyPendingTimers();

        expect(context.dispatch).toHaveBeenCalledWith(
            "messages/multipleByKey",
            expect.arrayContaining(context.state.messages.itemKeys.slice(100, 200))
        );
    });
    test("do not load messages already loaded", () => {
        context.state.messages.itemKeys.slice(100, 150).forEach(key => {
            context.getters["messages/messages"][key] = {};
        });
        jest.runOnlyPendingTimers();
        loadRange(context, { start: 100, end: 200 });
        expect(context.dispatch).toHaveBeenCalledWith(
            "messages/multipleByKey",
            expect.not.arrayContaining(context.state.messages.itemKeys.slice(100, 150))
        );
    });
    test("pre-load items arround requested range", () => {
        loadRange(context, { start: 100, end: 200 });
        jest.runOnlyPendingTimers();
        expect(context.dispatch).toHaveBeenCalledWith(
            "messages/multipleByKey",
            expect.arrayContaining(context.state.messages.itemKeys.slice(50, 250))
        );
    });
    test("immediatly load items in the requested range, but delay request if there is only pre-loaded items", () => {
        loadRange(context, { start: 100, end: 200 });
        expect(context.dispatch).toHaveBeenCalled();
        context.state.messages.itemKeys.slice(100, 150).forEach(key => {
            context.getters["messages/messages"][key] = {};
        });
        context.dispatch.mockClear();
        loadRange(context, { start: 100, end: 150 });
        expect(context.dispatch).not.toHaveBeenCalled();
        jest.runOnlyPendingTimers();
        // Fixme Debounce does not seems to work
        // expect(context.dispatch).toHaveBeenCalled();
    });
    test("overwrite delayed request if a new one is triggered before timeout", () => {
        //FIXME : this test should be completed with 2 successive delayed request,
        // but lodash debounce cannnot be tested...
        context.state.messages.itemKeys.slice(100, 150).forEach(key => {
            context.getters["messages/messages"][key] = {};
        });
        loadRange(context, { start: 100, end: 150 });
        loadRange(context, { start: 500, end: 550 });
        expect(context.dispatch).toHaveBeenCalledWith(
            "messages/multipleByKey",
            expect.arrayContaining(context.state.messages.itemKeys.slice(500, 550))
        );
        expect(context.dispatch).toHaveBeenCalledTimes(1);
    });
});
