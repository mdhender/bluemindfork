import { $_getIfNotPresent } from "../../actions/$_getIfNotPresent";

const context = {
    dispatch: jest.fn().mockReturnValue(Promise.resolve()),
    getters: {
        "messages/getMessagesByKey": jest.fn().mockReturnValue("SUCCESS")
    },
    rootGetters: {
        "mail/isLoaded": jest.fn().mockReturnValue(false)
    },
    commit: jest.fn()
};
describe("[Mail-WebappStore][actions] : $_getIfNotPresent", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
        context.getters["messages/getMessagesByKey"].mockClear();
        context.rootGetters["mail/isLoaded"].mockClear();
    });
    test("load message only if not in store", () => {
        const key1 = "key1",
            key2 = "key2";
        context.rootGetters["mail/isLoaded"].mockReturnValueOnce(true);
        $_getIfNotPresent(context, [key1]);
        expect(context.dispatch).toHaveBeenCalledWith("messages/multipleByKey", []);
        $_getIfNotPresent(context, [key2]);
        expect(context.dispatch).toHaveBeenCalledWith("messages/multipleByKey", [key2]);
    });
    test("return requested message", async () => {
        const key = "key";
        const message = await $_getIfNotPresent(context, [key]);
        expect(context.getters["messages/getMessagesByKey"]).toHaveBeenCalledWith([key]);
        expect(message).toBe("SUCCESS");
    });
    test("load only missing messages", async () => {
        const keys = ["key", "toto", "miam"];
        context.rootGetters["mail/isLoaded"].mockImplementation(key => key === "toto");
        await $_getIfNotPresent(context, keys);
        expect(context.dispatch).toHaveBeenCalledWith("messages/multipleByKey", ["key", "miam"]);
        expect(context.getters["messages/getMessagesByKey"]).toHaveBeenCalledWith(["key", "toto", "miam"]);
    });
});
