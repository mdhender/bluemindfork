import { purge as purgeAction } from "../../src/actions/purge";

let isMessageRemoveActionSuccessfull = true;
const context = {
    dispatch: jest.fn().mockImplementation(arg => {
        if (arg == "$_getIfNotPresent") {
            return Promise.resolve({});
        } else if (arg == "messages/remove") {
            return isMessageRemoveActionSuccessfull ? Promise.resolve({}) : Promise.reject();
        }
    }),
    commit: jest.fn()
};

describe("MailApp Store: Purge message action", () => {
    const messageKey = "message-key";

    const expectedLoadingAlert = {
        code: "MSG_PURGE_LOADING",
        props: { subject: undefined }
    };

    beforeEach(() => {
        context.dispatch.mockClear();
        context.commit.mockClear();
    });

    test("dispatch right actions and mutate alerts state when action is successful", done => {
        purgeAction(context, messageKey).then(() => {
            expect(context.dispatch).toHaveBeenCalledWith("$_getIfNotPresent", messageKey);
            expect(context.dispatch).toHaveBeenCalledWith("messages/remove", messageKey);

            expect(context.commit).toHaveBeenCalledTimes(3);
            let loadingAlertUid = context.commit.mock.calls[0][1].uid;

            expect(context.commit).toHaveBeenNthCalledWith(
                1,
                "alert/add",
                expect.objectContaining(expectedLoadingAlert),
                { root: true }
            );

            expect(context.commit).toHaveBeenNthCalledWith(2, "alert/remove", loadingAlertUid, { root: true });

            expect(context.commit).toHaveBeenNthCalledWith(
                3,
                "alert/add",
                expect.objectContaining({
                    code: "MSG_PURGE_OK",
                    props: { subject: undefined }
                }),
                { root: true }
            );
            done();
        });
    });

    test("if purge action fails, an error alert mutation is emitted", done => {
        isMessageRemoveActionSuccessfull = false;

        purgeAction(context, messageKey).then(() => {
            expect(context.dispatch).toHaveBeenCalledWith("$_getIfNotPresent", messageKey);
            expect(context.dispatch).toHaveBeenCalledWith("messages/remove", messageKey);

            expect(context.commit).toHaveBeenCalledTimes(3);
            let loadingAlertUid = context.commit.mock.calls[0][1].uid;

            expect(context.commit).toHaveBeenNthCalledWith(
                1,
                "alert/add",
                expect.objectContaining(expectedLoadingAlert),
                { root: true }
            );
            expect(context.commit).toHaveBeenNthCalledWith(2, "alert/remove", loadingAlertUid, { root: true });
            expect(context.commit).toHaveBeenNthCalledWith(
                3,
                "alert/add",
                expect.objectContaining({
                    code: "MSG_PURGE_ERROR",
                    props: { subject: undefined, reason: undefined }
                }),
                { root: true }
            );
            done();
        });
    });
});
