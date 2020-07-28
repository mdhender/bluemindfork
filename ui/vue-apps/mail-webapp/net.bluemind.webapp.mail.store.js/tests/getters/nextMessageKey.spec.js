import { nextMessageKey } from "../../src/getters/nextMessageKey";

describe("[Mail-WebappStore][getters] : nextMessageKey ", () => {
    const getters = {
        "messages/indexOf": jest.fn().mockReturnValue(1)
    };
    const state = {};
    beforeEach(() => {
        getters["messages/indexOf"].mockClear();
        getters["messages/count"] = 4;
        Object.assign(state, {
            currentMessage: { key: "whatever" },
            messages: { itemKeys: ["key1", "key2", "key3", "key4"] }
        });
    });
    test("return the next message using itemsKey order ", () => {
        const result = nextMessageKey(state, getters);
        expect(result).toBe("key3");
    });
    test("return the previous message if there is no next mesage ", () => {
        getters["messages/indexOf"].mockReturnValue(3);
        const result = nextMessageKey(state, getters);
        expect(result).toBe("key3");
    });
    test("to return null if there is no next messages", () => {
        getters["messages/count"] = 1;
        const result = nextMessageKey(state, getters);
        expect(result).toBeNull();
    });
});
