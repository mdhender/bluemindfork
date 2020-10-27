import ItemUri from "@bluemind/item-uri";

import { $_getIfNotPresent } from "../../actions/$_getIfNotPresent";

const context = {
    dispatch: jest.fn().mockReturnValue(Promise.resolve()),
    getters: {
        "messages/getMessagesByKey": jest.fn().mockReturnValue("SUCCESS")
    },
    rootGetters: {
        "mail/isLoaded": jest.fn().mockReturnValue(false)
    },
    rootState: {
        mail: {
            messages: {}
        }
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
        context.rootState.mail.messages = { key1: {}, key2: {} };

        $_getIfNotPresent(context, [key1]);
        expect(context.dispatch).toHaveBeenCalledWith("messages/multipleByKey", []);
        $_getIfNotPresent(context, [key2]);
        expect(context.dispatch).toHaveBeenCalledWith("messages/multipleByKey", [key2]);
    });
    test("return requested message", async () => {
        const key = "key";
        context.rootState.mail.messages = { key: {} };

        const message = await $_getIfNotPresent(context, [key]);
        expect(context.getters["messages/getMessagesByKey"]).toHaveBeenCalledWith([key]);
        expect(message).toBe("SUCCESS");
    });
    test("load only missing messages", async () => {
        const keyNotLoaded = ItemUri.encode("item", "folder");
        const keys = [keyNotLoaded, "key", "miam"];
        context.rootGetters["mail/isLoaded"].mockImplementation(key => key !== keyNotLoaded);
        context.rootState.mail.messages = { key: {}, miam: {} };

        await $_getIfNotPresent(context, keys);
        expect(context.dispatch).toHaveBeenCalledWith("messages/multipleByKey", [keyNotLoaded]);
        expect(context.getters["messages/getMessagesByKey"]).toHaveBeenCalledWith([keyNotLoaded, "key", "miam"]);
    });
});
