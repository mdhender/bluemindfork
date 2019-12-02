import { my } from "../../src/getters/my";

const getters = {
    "folders/getDefaultFolders": jest.fn(),
    "folders/getFoldersByMailbox": jest.fn()
};

describe("[Mail-WebappStore][getters] : my ", () => {
    beforeEach(() => {
        getters["folders/getDefaultFolders"].mockClear();
        getters["folders/getFoldersByMailbox"].mockClear();
    });
    test("return the defaults folder of the current user ", () => {
        const state = { login: "jane@bluemind.net" };
        my(state, getters);
        expect(getters["folders/getDefaultFolders"]).toHaveBeenCalledWith("user.jane");
    });
    test("return the current user mailbox UID ", () => {
        const state = { login: "jane@bluemind.net" };
        expect(my(state, getters).mailboxUid).toEqual("user.jane");
    });
    test("return the current user mailbox folders ", () => {
        const state = { login: "jane@bluemind.net" };
        my(state, getters);
        expect(getters["folders/getFoldersByMailbox"]).toHaveBeenCalledWith("user.jane");
    });
});
