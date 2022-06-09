import ServiceLocator from "@bluemind/inject";
import { MockMailboxItemsClient } from "@bluemind/test-utils";
import { attachment } from "@bluemind/mail";
import addAttachment from "../../actions/addAttachment";
import {
    ADD_ATTACHMENT,
    REMOVE_ATTACHMENT,
    SET_ATTACHMENT_ADDRESS,
    SET_ATTACHMENT_PROGRESS,
    SET_ATTACHMENT_STATUS,
    SET_MESSAGE_HAS_ATTACHMENT
} from "~/mutations";

const { AttachmentStatus } = attachment;

describe("addAttachment action", () => {
    global.URL.createObjectURL = jest.fn();
    const message = { key: "blabla", folderRef: { uid: "folder-uid" }, attachments: [] };
    const file = new Blob(["myfilecontentasastring"], {
        size: 22,
        type: "text/plain",
        name: "TestFile.txt"
    });

    const actionParams = {
        message,
        attachment: {},
        content: file
    };
    let context, mockedClient;

    beforeEach(() => {
        context = {
            commit: jest.fn()
        };

        mockedClient = new MockMailboxItemsClient();
        ServiceLocator.register({ provide: "MailboxItemsPersistence", factory: () => mockedClient });
    });

    test("Attach text file", async () => {
        await addAttachment(context, actionParams);
        expect(mockedClient.uploadPart).toHaveBeenCalled();
        expect(context.commit).toHaveBeenNthCalledWith(1, ADD_ATTACHMENT, expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(
            2,
            SET_ATTACHMENT_STATUS,
            expect.objectContaining({ messageKey: message.key, status: AttachmentStatus.NOT_UPLOADED })
        );
        expect(context.commit).toHaveBeenNthCalledWith(3, SET_MESSAGE_HAS_ATTACHMENT, {
            key: message.key,
            hasAttachment: true
        });
        expect(context.commit).toHaveBeenNthCalledWith(4, SET_ATTACHMENT_ADDRESS, expect.anything());
    });

    test("With error", async () => {
        mockedClient.uploadPart.mockImplementation(() => Promise.reject("error-reason"));
        await addAttachment(context, actionParams);
        expect(mockedClient.uploadPart).toHaveBeenCalled();
        expect(context.commit).toHaveBeenNthCalledWith(1, ADD_ATTACHMENT, expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(3, SET_MESSAGE_HAS_ATTACHMENT, {
            key: message.key,
            hasAttachment: true
        });
        expect(context.commit).toHaveBeenNthCalledWith(
            4,
            SET_ATTACHMENT_PROGRESS,
            expect.objectContaining({ messageKey: message.key, loaded: 100, total: 100 })
        );
        expect(context.commit).toHaveBeenNthCalledWith(
            5,
            SET_ATTACHMENT_STATUS,
            expect.objectContaining({ messageKey: message.key, status: AttachmentStatus.ERROR })
        );
    });

    test("Cancelled by user", async () => {
        mockedClient.uploadPart.mockImplementation(() => Promise.reject({ message: "CANCELLED_BY_CLIENT" }));
        jest.useFakeTimers();
        await addAttachment(context, actionParams);
        expect(mockedClient.uploadPart).toHaveBeenCalledWith(expect.anything(), expect.anything(), expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(1, ADD_ATTACHMENT, expect.anything());
        jest.runAllTimers();
        expect(context.commit).toHaveBeenNthCalledWith(4, REMOVE_ATTACHMENT, expect.anything());
    });
});
