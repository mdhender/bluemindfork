import { setCurrentMessage } from "../../src/mutations/setCurrentMessage";

describe.skip("[Mail-WebappStore][mutations] : setCurrentMessage ", () => {
    const state = {};
    beforeEach(() => {
        state.currentMessageKey = "key1";
    });
    test(" mutate currentMessageKey", () => {
        setCurrentMessage(state, "key2");
        expect(state.currentMessageKey).toEqual("key2");
    });
    test(" only mutate currentMessageKey ", () => {
        setCurrentMessage(state, "key2");
        expect(Object.keys(state)).toEqual(["currentMessageKey"]);
    });
});
