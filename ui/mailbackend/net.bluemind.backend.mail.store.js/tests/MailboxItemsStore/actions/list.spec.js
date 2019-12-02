import { list } from "../../../src/MailboxItemsStore/actions/list";
import ServiceLocator from "@bluemind/inject";

jest.mock("@bluemind/inject");

const result = [1, 2, 3];
const sortedIds = jest.fn().mockReturnValue(Promise.resolve(result));
const get = jest.fn().mockReturnValue({
    sortedIds
});
ServiceLocator.getProvider.mockReturnValue({
    get
});

const context = {
    commit: jest.fn()
};

describe("[MailItemsStore][actions] : list", () => {
    beforeEach(() => {
        context.commit.mockClear();
    });
    test("call sortedIds for the given folder and mutate state with result", done => {
        const sorted = "sorted",
            folderUid = "containerUid";
        list(context, { sorted, folderUid }).then(() => {
            expect(context.commit).toHaveBeenCalledWith("setItemKeys", { ids: result, folderUid });
            done();
        });
        expect(get).toHaveBeenCalledWith("containerUid");
        expect(sortedIds).toHaveBeenCalledWith(sorted);
    });
    test("fail if sortedIds call fail", () => {
        const sorted = "sorted",
            containerUid = "containerUid";
        sortedIds.mockReturnValueOnce(Promise.reject("Error!"));
        expect(list(context, { sorted, containerUid })).rejects.toBe("Error!");
    });
});
