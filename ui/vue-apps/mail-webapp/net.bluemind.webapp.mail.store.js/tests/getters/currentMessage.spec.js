import { currentMessage } from "../../src/getters/currentMessage";

const getters = {
    "messages/getMessageByKey": jest.fn().mockReturnValue("TheMessage")
};

const state = { currentMessageKey: "key" };

describe("[Mail-WebappStore][getters] : currentMessage ", () => {
    test("return current Message instance ", () => {
        const result = currentMessage(state, getters);
        expect(getters["messages/getMessageByKey"]).toHaveBeenCalledWith("key");
        expect(result).toEqual("TheMessage");
    });
});
