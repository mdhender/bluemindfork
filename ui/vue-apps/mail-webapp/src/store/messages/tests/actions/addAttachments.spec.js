import ServiceLocator from "@bluemind/inject";
import { MockMailboxItemsClient } from "@bluemind/test-utils";

import addAttachments from "../../actions/addAttachments";
import { AttachmentStatus } from "../../../../model/attachment";
import {
    ADD_ATTACHMENT,
    REMOVE_ATTACHMENT,
    SET_ATTACHMENT_ADDRESS,
    SET_ATTACHMENT_PROGRESS,
    SET_ATTACHMENT_STATUS
} from "~mutations";

describe("addAttachments action", () => {
    global.URL.createObjectURL = jest.fn();
    const messageKey = "blabla";
    const files = [
        new Blob(["myfilecontentasastring"], {
            size: 22,
            type: "text/plain"
        })
    ];
    files.name = "TestFile.txt";

    const actionParams = { messageKey, files, userPrefTextOnly: false, messageCompose: {} };
    let context, mockedClient;

    beforeEach(() => {
        context = {
            commit: jest.fn(),
            dispatch: jest.fn().mockReturnValue(Promise.resolve()),
            state: {
                [messageKey]: {
                    folderRef: { uid: "folder-uid" }
                }
            }
        };

        mockedClient = new MockMailboxItemsClient();
        ServiceLocator.register({ provide: "MailboxItemsPersistence", factory: () => mockedClient });
    });

    test("Attach text file", async () => {
        await addAttachments(context, actionParams);
        expect(mockedClient.uploadPart).toHaveBeenCalledWith(expect.anything(), expect.anything(), expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(1, ADD_ATTACHMENT, expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(2, SET_ATTACHMENT_ADDRESS, expect.anything());
    });

    test("With error", async () => {
        mockedClient.uploadPart.mockImplementation(() => Promise.reject("error-reason"));
        await addAttachments(context, actionParams);
        expect(mockedClient.uploadPart).toHaveBeenCalledWith(expect.anything(), expect.anything(), expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(1, ADD_ATTACHMENT, expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(
            2,
            SET_ATTACHMENT_PROGRESS,
            expect.objectContaining({ messageKey, loaded: 100, total: 100 })
        );
        expect(context.commit).toHaveBeenNthCalledWith(
            3,
            SET_ATTACHMENT_STATUS,
            expect.objectContaining({ messageKey, status: AttachmentStatus.ERROR })
        );
    });

    test("Cancelled by user", async () => {
        mockedClient.uploadPart.mockImplementation(() => Promise.reject({ message: "CANCELLED_BY_CLIENT" }));
        jest.useFakeTimers();
        await addAttachments(context, actionParams);
        expect(mockedClient.uploadPart).toHaveBeenCalledWith(expect.anything(), expect.anything(), expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(1, ADD_ATTACHMENT, expect.anything());
        jest.runAllTimers();
        expect(context.commit).toHaveBeenNthCalledWith(2, REMOVE_ATTACHMENT, expect.anything());
    });
});
