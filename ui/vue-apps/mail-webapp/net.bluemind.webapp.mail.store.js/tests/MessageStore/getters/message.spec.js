import { message } from "../../../src/MessageStore/getters/message";

const rootGetters = {
    "mail-webapp/messages/getMessageByKey": jest.fn().mockReturnValue("TheMessage")
};

const state = { key: "key" };

describe("[Mail-WebappStore/MessageStore][getters] : message ", () => {
    test("return Message instance ", () => {
        const result = message(state, undefined, undefined, rootGetters);
        expect(rootGetters["mail-webapp/messages/getMessageByKey"]).toHaveBeenCalledWith("key");
        expect(result).toEqual("TheMessage");
    });
});
