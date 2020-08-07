import { loadRange } from "../../actions/loadRange";

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
    test("load messages depending on sorted key order", async () => {
        await loadRange(context, { start: 100, end: 200 });

        expect(context.dispatch).toHaveBeenCalledWith(
            "messages/multipleByKey",
            expect.arrayContaining(context.state.messages.itemKeys.slice(100, 200))
        );
    });
    test("do not load messages already loaded", async () => {
        context.state.messages.itemKeys.slice(100, 150).forEach(key => {
            context.getters["messages/messages"][key] = {};
        });
        await loadRange(context, { start: 100, end: 200 });
        expect(context.dispatch).toHaveBeenCalledWith(
            "messages/multipleByKey",
            expect.not.arrayContaining(context.state.messages.itemKeys.slice(100, 150))
        );
    });
    test("pre-load items arround requested range", async () => {
        await loadRange(context, { start: 100, end: 200 });
        expect(context.dispatch).toHaveBeenCalledWith(
            "messages/multipleByKey",
            expect.arrayContaining(context.state.messages.itemKeys.slice(50, 250))
        );
    });
    test("Do not load item aleady requested but not yet return by remote", async () => {
        loadRange(context, { start: 100, end: 200 });
        context.dispatch.mockClear();
        loadRange(context, { start: 0, end: 500 });
        expect(context.dispatch).toHaveBeenCalledWith(
            "messages/multipleByKey",
            expect.not.arrayContaining(context.state.messages.itemKeys.slice(100, 200))
        );
    });
    test("Pre-load next range if not loaded", async () => {
        context.state.messages.itemKeys.slice(100, 200).forEach(key => {
            context.getters["messages/messages"][key] = {};
        });
        await loadRange(context, { start: 125, end: 150 });
        expect(context.dispatch).not.toHaveBeenCalled();
        await loadRange(context, { start: 100, end: 200 });
        expect(context.dispatch).toHaveBeenCalledWith(
            "messages/multipleByKey",
            expect.arrayContaining(
                context.state.messages.itemKeys.slice(0, 100).concat(context.state.messages.itemKeys.slice(200, 300))
            )
        );
        context.dispatch.mockClear();
        await loadRange(context, { start: 175, end: 200 });
        expect(context.dispatch).toHaveBeenCalledWith(
            "messages/multipleByKey",
            expect.arrayContaining(context.state.messages.itemKeys.slice(200, 225))
        );
    });
});
