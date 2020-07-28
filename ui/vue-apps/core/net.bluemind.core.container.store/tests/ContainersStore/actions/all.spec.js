import { all as allAction } from "../../../src/ContainersStore/actions/all";
import ServiceLocator from "@bluemind/inject";

jest.mock("@bluemind/inject");

const result = [1, 2, 3];
const all = jest.fn().mockReturnValue(Promise.resolve(result));
const get = jest.fn().mockReturnValue({
    all
});
ServiceLocator.getProvider.mockReturnValue({
    get
});

const context = {
    commit: jest.fn()
};

describe("[ContainersStore][actions] : all", () => {
    beforeEach(() => {
        context.commit.mockClear();
    });
    test("call 'all' service and mutate state with result", done => {
        allAction(context, { type: "T", verb: "V" }).then(() => {
            expect(context.commit).toHaveBeenCalledWith("storeContainers", result);
            done();
        });
        expect(ServiceLocator.getProvider).toHaveBeenCalledWith("ContainersPersistence");
        expect(get).toHaveBeenCalledWith();
        expect(all).toHaveBeenCalledWith({ type: "T", verb: "V" });
    });
    test("fail if 'all' call fail", () => {
        all.mockReturnValueOnce(Promise.reject("Error!"));
        expect(allAction(context, { type: "T", verb: "V" })).rejects.toBe("Error!");
    });
});
