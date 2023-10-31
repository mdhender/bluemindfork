import { extend } from "../src/";

const store = "Store!";
const next = jest.fn();
const VueRouterMock = {
    callbacks: [],
    next,
    beforeEach(callback) {
        this.callbacks.push(callback);
    },
    async navigate(from, to) {
        for (let callback of this.callbacks) {
            await callback(to, from, next);
        }
    }
};

function createRoute(path) {
    return {
        path,
        meta: {
            onUpdate: jest.fn().mockResolvedValue(),
            onEnter: jest.fn().mockResolvedValue(),
            onLeave: jest.fn().mockResolvedValue(),
            watch: {
                updated: jest.fn().mockResolvedValue(),
                old: jest.fn().mockResolvedValue(),
                neo: jest.fn().mockResolvedValue(),
                unchanged: jest.fn().mockResolvedValue(),
                absent: jest.fn().mockResolvedValue()
            }
        }
    };
}
function clearRouteMocks(route) {
    route.meta.onUpdate.mockClear();
    route.meta.onEnter.mockClear();
    route.meta.onLeave.mockClear();
    route.meta.watch.updated.mockClear();
    route.meta.watch.unchanged.mockClear();
    route.meta.watch.absent.mockClear();
    route.meta.watch.old.mockClear();
    route.meta.watch.neo.mockClear();
}

const ENTERING = createRoute("/in"),
    LEAVING = createRoute("/out"),
    UPDATING = createRoute("/here"),
    NEVER = createRoute("/there");

const from = {
    params: {
        old: "Old",
        updated: "Old",
        unchanged: "Constant"
    },
    matched: [UPDATING, LEAVING]
};
const to = {
    params: {
        neo: "New",
        updated: "New",
        unchanged: "Constant"
    },
    matched: [UPDATING, ENTERING]
};
// const next = () => {};

describe("VuexRouterPlugin", () => {
    beforeAll(() => {
        extend(VueRouterMock, store);
    });
    beforeEach(() => {
        [ENTERING, LEAVING, UPDATING, NEVER].forEach(clearRouteMocks);
        VueRouterMock.next.mockClear();
    });

    test("execute onEnter hook when a route is matched for the first time", async () => {
        await VueRouterMock.navigate(from, to);
        expect(ENTERING.meta.onEnter).toHaveBeenCalledTimes(1);
        expect(ENTERING.meta.onEnter).toHaveBeenNthCalledWith(1, store, to, from);
        expect(UPDATING.meta.onEnter).not.toHaveBeenCalled();
        expect(LEAVING.meta.onEnter).not.toHaveBeenCalled();
        expect(NEVER.meta.onEnter).not.toHaveBeenCalled();
        await VueRouterMock.navigate(to, to);
        expect(UPDATING.meta.onEnter).not.toHaveBeenCalled();
        expect(NEVER.meta.onUpdate).not.toHaveBeenCalled();
    });
    test("execute onUpdate hook each time a route is matched", async () => {
        await VueRouterMock.navigate(from, to);
        expect(ENTERING.meta.onUpdate).toHaveBeenCalledTimes(1);
        expect(ENTERING.meta.onUpdate).toHaveBeenNthCalledWith(1, store, to, from, next);
        expect(UPDATING.meta.onUpdate).toHaveBeenCalledTimes(1);
        expect(UPDATING.meta.onUpdate).toHaveBeenNthCalledWith(1, store, to, from, next);
        expect(LEAVING.meta.onUpdate).not.toHaveBeenCalled();
        expect(NEVER.meta.onUpdate).not.toHaveBeenCalled();
        await VueRouterMock.navigate(to, to);
        expect(UPDATING.meta.onUpdate).toHaveBeenCalledTimes(2);
        expect(UPDATING.meta.onUpdate).toHaveBeenNthCalledWith(2, store, to, to, next);
        expect(NEVER.meta.onUpdate).not.toHaveBeenCalled();
    });
    test("execute onLeave hook when a route is not matched anymore", async () => {
        await VueRouterMock.navigate(from, to);
        expect(ENTERING.meta.onLeave).not.toHaveBeenCalled();
        expect(UPDATING.meta.onLeave).not.toHaveBeenCalled();
        expect(LEAVING.meta.onLeave).toHaveBeenCalledTimes(1);
        expect(LEAVING.meta.onLeave).toHaveBeenNthCalledWith(1, store, to, from);
        expect(NEVER.meta.onLeave).not.toHaveBeenCalled();
        await VueRouterMock.navigate(to, to);
        expect(UPDATING.meta.onLeave).not.toHaveBeenCalled();
        expect(NEVER.meta.onLeave).not.toHaveBeenCalled();
    });
    test("execute watch hook when a parameter changes on matched route", async () => {
        await VueRouterMock.navigate(from, to);
        expect(ENTERING.meta.watch.updated).not.toHaveBeenCalled();
        expect(UPDATING.meta.watch.updated).toHaveBeenCalled();
        expect(LEAVING.meta.watch.updated).not.toHaveBeenCalled();
        expect(NEVER.meta.watch.updated).not.toHaveBeenCalled();

        expect(ENTERING.meta.watch.unchanged).not.toHaveBeenCalled();
        expect(UPDATING.meta.watch.unchanged).not.toHaveBeenCalled();
        expect(LEAVING.meta.watch.unchanged).not.toHaveBeenCalled();
        expect(NEVER.meta.watch.unchanged).not.toHaveBeenCalled();

        expect(ENTERING.meta.watch.absent).not.toHaveBeenCalled();
        expect(UPDATING.meta.watch.absent).not.toHaveBeenCalled();
        expect(LEAVING.meta.watch.absent).not.toHaveBeenCalled();
        expect(NEVER.meta.watch.absent).not.toHaveBeenCalled();

        expect(ENTERING.meta.watch.old).not.toHaveBeenCalled();
        expect(UPDATING.meta.watch.old).toHaveBeenCalled();
        expect(LEAVING.meta.watch.old).not.toHaveBeenCalled();
        expect(NEVER.meta.watch.old).not.toHaveBeenCalled();

        expect(ENTERING.meta.watch.neo).not.toHaveBeenCalled();
        expect(UPDATING.meta.watch.neo).toHaveBeenCalled();
        expect(LEAVING.meta.watch.neo).not.toHaveBeenCalled();
        expect(NEVER.meta.watch.neo).not.toHaveBeenCalled();
    });
    test("execute onEnter hook before onUpdate et after onLeave", async () => {
        let order = 1;
        ENTERING.meta.onEnter.mockImplementation(async () => expect(order++).toBe(2));
        ENTERING.meta.onUpdate.mockImplementation(async () => expect(order++).toBe(3));
        LEAVING.meta.onLeave.mockImplementation(async () => expect(order++).toBe(1));
        await VueRouterMock.navigate(from, to);
        expect.assertions(3);
    });
});
