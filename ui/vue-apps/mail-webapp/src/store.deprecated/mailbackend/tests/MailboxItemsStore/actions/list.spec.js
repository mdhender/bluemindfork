import { list } from "../../../MailboxItemsStore/actions/list";
import actionTypes from "../../../../../store/actionTypes";

//FIXME: Something is wrong if I need to mock dispatch
const context = {
    commit: jest.fn(),
    dispatch: jest.fn(),
    rootState: {
        mail: {
            folders: {
                containerUid: {}
            }
        },
        session: { userSettings: {} }
    }
};

describe("[MailItemsStore][actions] : list", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
    });
    test("call sortedIds for the given folder and mutate state with result", () => {
        list(context, { folderUid: "containerUid" });
        expect(context.dispatch).toHaveBeenCalledWith(
            "mail/" + actionTypes.FETCH_FOLDER_MESSAGE_KEYS,
            { folder: {}, filter: undefined, conversationsEnabled: false },
            { root: true }
        );
    });
    test("fail if sortedIds call fail", () => {
        context.dispatch.mockReturnValueOnce(Promise.reject("Error!"));
        expect(list(context, { folderUid: "containerUid" })).rejects.toBe("Error!");
    });
    test("call sortedIds when 'all' filter is set", () => {
        list(context, { folderUid: "containerUid", filter: "all" });
        expect(context.dispatch).toHaveBeenCalledWith(
            "mail/" + actionTypes.FETCH_FOLDER_MESSAGE_KEYS,
            { folder: {}, filter: "all", conversationsEnabled: false },
            { root: true }
        );
    });
});
