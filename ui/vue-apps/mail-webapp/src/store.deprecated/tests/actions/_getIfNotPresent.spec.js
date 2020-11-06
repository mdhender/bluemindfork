import ItemUri from "@bluemind/item-uri";

import { $_getIfNotPresent } from "../../actions/$_getIfNotPresent";

describe("[Mail-WebappStore][actions] : $_getIfNotPresent", () => {
    let context;

    beforeEach(() => {
        context = {
            dispatch: jest.fn().mockImplementation((actionName, keys) => {
                keys.forEach(key => {
                    context.rootState.mail.messages[key] = { key };
                });
                return Promise.resolve();
            }),
            rootGetters: {
                "mail/isLoaded": jest
                    .fn()
                    .mockImplementation(key =>
                        Object.prototype.hasOwnProperty.call(context.rootState.mail.messages, key)
                    )
            },
            rootState: {
                mail: {
                    messages: {}
                }
            },
            commit: jest.fn()
        };
    });

    test("load message only if not in state", async () => {
        const key1 = ItemUri.encode("item1", "folder1"),
            key2 = ItemUri.encode("item1", "folder2");
        context.rootState.mail.messages = { [key1]: {} };

        await $_getIfNotPresent(context, [key1]);
        expect(context.dispatch).toHaveBeenCalledWith("messages/multipleByKey", []);
        await $_getIfNotPresent(context, [key2]);
        expect(context.dispatch).toHaveBeenCalledWith("messages/multipleByKey", [key2]);
    });

    test("return requested message", async () => {
        const key = "key";
        context.rootState.mail.messages[key] = { key, anyprop: "SUCCESS" };

        const message = (await $_getIfNotPresent(context, [key]))[0];
        expect(message.anyprop).toBe("SUCCESS");
        expect(message.key).toBe(key);
    });

    test("load only missing messages", async () => {
        const keyNotLoaded = ItemUri.encode("item", "folder");
        const keys = [keyNotLoaded, "key", "miam"];
        context.rootGetters["mail/isLoaded"].mockImplementation(key => key !== keyNotLoaded);
        keys.forEach(key => {
            if (key !== keyNotLoaded) {
                context.rootState.mail.messages[key] = { key };
            }
        });

        const messages = await $_getIfNotPresent(context, keys);
        expect(context.dispatch).toHaveBeenCalledWith("messages/multipleByKey", [keyNotLoaded]);
        expect(messages.length).toBe(keys.length);
        expect(messages.map(m => m.key)).toEqual(keys);
    });
});
