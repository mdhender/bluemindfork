import { purge as purgeAction } from "../../src/actions/purge";
import { AlertTypes } from "@bluemind/alert.store";

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
    
    const messageId = 17289, 
        folderUid = "2da34601-8c78-4cc3-baf0-1ae3dfe24a23/17289";
    
    const expectedLoadingAlert = {
        type: AlertTypes.LOADING,
        code: "ALERT_CODE_MSG_PURGE_LOADING",
        props: { subject: undefined }
    };

    beforeEach(() => {
        context.dispatch.mockClear();
        context.commit.mockClear();
    });

    test("dispatch right actions and mutate alerts state when action is successful", done => {
        purgeAction(context, { messageId, folderUid }).then(() => {
            expect(context.dispatch).toHaveBeenCalledWith("$_getIfNotPresent", { folder: folderUid, id: messageId });
            expect(context.dispatch).toHaveBeenCalledWith("messages/remove", { messageId, folderUid });
            
            expect(context.commit).toHaveBeenCalledTimes(3);
            let loadingAlertUid = context.commit.mock.calls[0][1].uid;

            expect(context.commit).toHaveBeenNthCalledWith(1, "alert/addAlert", 
                expect.objectContaining(expectedLoadingAlert),  {"root": true});

            expect(context.commit).toHaveBeenNthCalledWith(2, "alert/removeAlert", loadingAlertUid, {"root": true});

            expect(context.commit).toHaveBeenNthCalledWith(3, "alert/addAlert", expect.objectContaining({
                type: AlertTypes.SUCCESS,
                code: "ALERT_CODE_MSG_PURGE_OK",
                props: { subject: undefined }
            }), {"root": true});
            done();
        });
    });

    test("if purge action fails, an error alert mutation is emitted", done => {
        isMessageRemoveActionSuccessfull = false;

        purgeAction(context, { messageId, folderUid }).then(() => {
            expect(context.dispatch).toHaveBeenCalledWith("$_getIfNotPresent", { folder: folderUid, id: messageId });
            expect(context.dispatch).toHaveBeenCalledWith("messages/remove", { messageId, folderUid });
            
            expect(context.commit).toHaveBeenCalledTimes(3);
            let loadingAlertUid = context.commit.mock.calls[0][1].uid;

            expect(context.commit).toHaveBeenNthCalledWith(1, "alert/addAlert", 
                expect.objectContaining(expectedLoadingAlert),  {"root": true});
            expect(context.commit).toHaveBeenNthCalledWith(2, "alert/removeAlert", loadingAlertUid, {"root": true});
            expect(context.commit).toHaveBeenNthCalledWith(3, "alert/addAlert", expect.objectContaining({
                type: AlertTypes.ERROR,
                code: "ALERT_CODE_MSG_PURGE_ERROR",
                props: { subject: undefined, reason: undefined }
            }), {"root": true});
            done();
        });
    });
});