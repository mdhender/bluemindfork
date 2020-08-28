import { nextMessageKey } from "../../getters/nextMessageKey";

describe("[Mail-WebappStore][getters] : nextMessageKey ", () => {
    const getters = {
        "messages/indexOf": jest.fn(key => {
            switch (key) {
                case "key1":
                    return 0;
                case "key2":
                    return 1;
                case "key3":
                    return 2;
                case "key4":
                    return 3;
                default:
                    return -1;
            }
        })
    };
    const state = { selectedMessageKeys: [] };
    const rootState = {
        mail: {
            messageList: {
                messageKeys: []
            }
        }
    };

    beforeEach(() => {
        getters["messages/indexOf"].mockClear();
        getters["messages/count"] = 4;
        Object.assign(state, {
            currentMessage: { key: "whatever" }
        });
        rootState.mail.messageList.messageKeys = ["key1", "key2", "key3", "key4"];
        state.selectedMessageKeys = [];
    });
    test("return the next message using itemsKey order ", () => {
        state.currentMessage.key = "key2";
        const result = nextMessageKey(state, getters, rootState);
        expect(result).toBe("key3");
    });
    test("return the previous message if there is no next mesage ", () => {
        state.currentMessage.key = "key4";
        const result = nextMessageKey(state, getters, rootState);
        expect(result).toBe("key3");
    });
    test("to return undefined if there is no next messages", () => {
        getters["messages/count"] = 1;
        const result = nextMessageKey(state, getters, rootState);
        expect(result).toBeUndefined();
    });
    test("return the next message with key1 and key2 selected", () => {
        state.selectedMessageKeys = ["key2", "key1"];
        const result = nextMessageKey(state, getters, rootState);
        expect(result).toBe("key3");
    });
    test("return the next message with key3 and key4 selected", () => {
        state.selectedMessageKeys = ["key4", "key3"];
        const result = nextMessageKey(state, getters, rootState);
        expect(result).toBe("key2");
    });
    test("return the next message with key2 and key4 selected", () => {
        state.selectedMessageKeys = ["key2", "key4"];
        const result = nextMessageKey(state, getters, rootState);
        expect(result).toBe("key1");
    });
    test("return the next message with key1 and key4 selected", () => {
        state.selectedMessageKeys = ["key4", "key1"];
        const result = nextMessageKey(state, getters, rootState);
        expect(result).toBe("key2");
    });
    test("return the next message with key2 and key3 selected", () => {
        state.selectedMessageKeys = ["key3", "key2"];
        const result = nextMessageKey(state, getters, rootState);
        expect(result).toBe("key4");
    });
    test("return undefined when all keys are selected", () => {
        state.selectedMessageKeys = ["key3", "key2", "key4", "key1"];
        const result = nextMessageKey(state, getters, rootState);
        expect(result).toBeUndefined();
    });
});
