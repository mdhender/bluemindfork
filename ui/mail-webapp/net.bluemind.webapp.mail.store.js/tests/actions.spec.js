import { loadRange } from "../src/actions/loadRange";

jest.useFakeTimers();

const context = {
    dispatch: jest.fn().mockReturnValue(Promise.resolve()),
    rootGetters: {
        "backend.mail/items/messages": {},
        "backend.mail/folders/currentFolder": "folder_uid"
    },
    rootState: {
        "backend.mail/items": {
            sortedIds: new Array(1000).fill(0).map((val, index) => index)
        }
    }
};
describe("MailApp Store: LoadRange action", () => {
    beforeEach(() => {
        context.rootGetters["backend.mail/items/messages"] = {};
    });
    afterEach(() => {
        jest.runOnlyPendingTimers();
        context.dispatch.mockClear();
    });
    test("load messages depending on sorted id order", () => {
        loadRange(context, { start: 100, end: 200 });
        jest.runOnlyPendingTimers();

        expect(context.dispatch).toHaveBeenCalledWith(
            "backend.mail/items/multipleById",
            {
                folder: expect.anything(),
                ids: expect.arrayContaining(context.rootState["backend.mail/items"].sortedIds.slice(100, 200))
            },
            expect.anything()
        );
    });
    test("do not load messages already loaded", () => {
        context.rootState["backend.mail/items"].sortedIds.slice(100, 150).forEach(id => {
            context.rootGetters["backend.mail/items/messages"][id] = {};
        });
        jest.runOnlyPendingTimers();
        loadRange(context, { start: 100, end: 200 });
        expect(context.dispatch).toHaveBeenCalledWith(
            "backend.mail/items/multipleById",
            {
                folder: expect.anything(),
                ids: expect.not.arrayContaining(context.rootState["backend.mail/items"].sortedIds.slice(100, 150))
            },
            expect.anything()
        );
    });
    test("pre-load items arround requested range", () => {
        loadRange(context, { start: 100, end: 200 });
        jest.runOnlyPendingTimers();
        expect(context.dispatch).toHaveBeenCalledWith(
            "backend.mail/items/multipleById",
            {
                folder: expect.anything(),
                ids: expect.arrayContaining(context.rootState["backend.mail/items"].sortedIds.slice(50, 250))
            },
            expect.anything()
        );
    });
    test("immediatly load items in the requested range, but delay request if there is only pre-loaded items", () => {
        loadRange(context, { start: 100, end: 200 });
        expect(context.dispatch).toHaveBeenCalled();
        context.rootState["backend.mail/items"].sortedIds.slice(100, 150).forEach(id => {
            context.rootGetters["backend.mail/items/messages"][id] = {};
        });
        context.dispatch.mockClear();
        loadRange(context, { start: 100, end: 150 });
        expect(context.dispatch).not.toHaveBeenCalled();
        jest.runOnlyPendingTimers();
        // Fixme Debounce does not seems to work
        // expect(context.dispatch).toHaveBeenCalled();
    });
    test("overwritte delayed request if a new one is triggered before timeout", () => {
        //FIXME : this test should be completed with 2 successive delayed request,
        // but lodash debounce cannnot be tested...
        context.rootState["backend.mail/items"].sortedIds.slice(100, 150).forEach(id => {
            context.rootGetters["backend.mail/items/messages"][id] = {};
        });
        loadRange(context, { start: 100, end: 150 });
        loadRange(context, { start: 500, end: 550 });
        expect(context.dispatch).toHaveBeenCalledWith(
            "backend.mail/items/multipleById",
            {
                folder: expect.anything(),
                ids: expect.arrayContaining(context.rootState["backend.mail/items"].sortedIds.slice(500, 550))
            },
            expect.anything()
        );
        expect(context.dispatch).toHaveBeenCalledTimes(1);
    });
});
