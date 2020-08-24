import { list } from "../../../MailboxItemsStore/actions/list";
import ServiceLocator from "@bluemind/inject";

jest.mock("@bluemind/inject");

const result = [1, 2, 3];
const filteredResult = { created: [{ id: 2 }] };
const sortedIds = jest.fn().mockReturnValue(Promise.resolve(result));
const unreadItems = jest.fn().mockReturnValue(Promise.resolve(result));
const filteredChangesetById = jest.fn().mockReturnValue(Promise.resolve(filteredResult));
const get = jest.fn().mockReturnValue({
    sortedIds,
    filteredChangesetById,
    unreadItems
});
ServiceLocator.getProvider.mockReturnValue({
    get
});

//FIXME: Something is wrong if I need to mock dispatch
const context = {
    commit: jest.fn(),
    dispatch: jest.fn()
};

describe("[MailItemsStore][actions] : list", () => {
    beforeEach(() => {
        context.commit.mockClear();
    });
    test("call sortedIds for the given folder and mutate state with result", done => {
        const sorted = "sorted",
            folderUid = "containerUid";
        list(context, { sorted, folderUid }).then(() => {
            expect(context.commit).toHaveBeenCalledWith("setItemKeysByIdsFolderUid", { ids: result, folderUid });
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
    test("call sortedIds when 'all' filter is set", done => {
        const sorted = "sorted",
            folderUid = "containerUid";
        list(context, { sorted, folderUid, filter: "all" }).then(() => {
            expect(context.commit).toHaveBeenCalledWith("setItemKeysByIdsFolderUid", { ids: result, folderUid });
            done();
        });
        expect(get).toHaveBeenCalledWith("containerUid");
        expect(sortedIds).toHaveBeenCalledWith(sorted);
    });
    test("call unreadItems when 'unread' filter is set", done => {
        const sorted = { column: "internal_date", dir: "Desc" },
            folderUid = "containerUid";
        list(context, { sorted, folderUid, filter: "unread" }).then(() => {
            expect(context.commit).toHaveBeenCalledWith("setItemKeysByIdsFolderUid", { ids: result, folderUid });
            done();
        });
        expect(get).toHaveBeenCalledWith("containerUid");
        expect(unreadItems).toHaveBeenCalled();
    });
});
