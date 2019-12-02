import { $_getIfNotPresent } from "../../src/actions/$_getIfNotPresent";

const context = {
    dispatch: jest.fn().mockReturnValue(Promise.resolve()),
    getters: {
        "messages/getMessageByKey": jest.fn().mockReturnValue("SUCCESS")
    },
    state: {
        currentFolderUid: "folder_uid",
        messages: {
            items: {}
        }
    }
};
describe("[Mail-WebappStore][actions] : $_getIfNotPresent", () => {
    beforeEach(() => {
        context.state.messages.items = {};
        context.dispatch.mockClear();
        context.getters["messages/getMessageByKey"].mockClear();
    });
    test("load message only if not in store", () => {
        const key1 = "key1",
            key2 = "key2";
        context.state.messages.items[key1] = {};
        $_getIfNotPresent(context, key1);
        expect(context.dispatch).not.toHaveBeenCalled();
        $_getIfNotPresent(context, key2);
        expect(context.dispatch).toHaveBeenCalledWith("messages/getCompleteByKey", key2);
    });
    test("return requested message", done => {
        const key = "key";
        $_getIfNotPresent(context, key).then(message => {
            expect(context.getters["messages/getMessageByKey"]).toHaveBeenCalledWith(key);
            expect(message).toBe("SUCCESS");
            done();
        });
    });
});
