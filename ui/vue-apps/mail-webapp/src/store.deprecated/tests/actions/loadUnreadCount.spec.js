import { loadUnreadCount } from "../../actions/loadUnreadCount";
import ServiceLocator from "@bluemind/inject";

jest.mock("@bluemind/inject");

const getPerUserUnread = jest.fn().mockReturnValue(Promise.resolve({ total: 10 }));
const get = jest.fn().mockReturnValue({
    getPerUserUnread
});
ServiceLocator.getProvider.mockReturnValue({
    get
});

const context = {
    commit: jest.fn()
};

describe("[Mail-WebappStore][actions] : loadUnreadCount", () => {
    beforeEach(() => {
        context.commit.mockClear();
    });
    test("Call getPerUnserUnread for the given folder and mutate state with result", done => {
        loadUnreadCount(context, "folderUid").then(() => {
            expect(context.commit).toHaveBeenCalledWith(
                "mail/SET_UNREAD_COUNT",
                { key: "folderUid", count: 10 },
                { root: true }
            );
            done();
        });
        expect(get).toHaveBeenCalledWith("folderUid");
        expect(getPerUserUnread).toHaveBeenCalled();
    });
});
