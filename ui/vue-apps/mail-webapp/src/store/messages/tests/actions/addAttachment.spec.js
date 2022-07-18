import ServiceLocator from "@bluemind/inject";
import { MockMailboxItemsClient } from "@bluemind/test-utils";
import { fileUtils } from "@bluemind/mail";
import addAttachment from "../../actions/addAttachment";
import {
    ADD_ATTACHMENT,
    ADD_FILE,
    REMOVE_ATTACHMENT,
    REMOVE_FILE,
    SET_ATTACHMENT_ADDRESS,
    SET_FILE_PROGRESS,
    SET_FILE_STATUS,
    SET_MESSAGE_HAS_ATTACHMENT
} from "~/mutations";

const { FileStatus } = fileUtils;
ServiceLocator.register({ provide: "i18n", use: { t: n => n } });

describe("addAttachment action", () => {
    global.URL.createObjectURL = jest.fn();
    const message = {
        key: "blabla",
        folderRef: { uid: "folder-uid" },
        remoteRef: { uid: "remote-uid" },
        attachments: []
    };
    const file = new Blob(["myfilecontentasastring"], {
        size: 22,
        type: "text/plain",
        name: "TestFile.txt"
    });

    const actionParams = {
        message,
        attachment: {
            address: "1733A829-2AD8-4DA8-B185-E07DCA845A60",
            charset: "us-ascii",
            dispositionType: "ATTACHMENT",
            encoding: "base64",
            fileName: "change_folder.gif",
            mime: "image/gif",
            progress: { loaded: 0, total: 100 },
            size: 2934590,
            status: "NOT-LOADED"
        },
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
        expect(context.commit).toHaveBeenNthCalledWith(1, ADD_FILE, expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(2, ADD_ATTACHMENT, expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(
            3,
            SET_FILE_STATUS,
            expect.objectContaining({ status: FileStatus.NOT_LOADED })
        );
        expect(context.commit).toHaveBeenNthCalledWith(4, SET_MESSAGE_HAS_ATTACHMENT, {
            key: message.key,
            hasAttachment: true
        });
        expect(context.commit).toHaveBeenNthCalledWith(5, SET_ATTACHMENT_ADDRESS, expect.anything());
    });

    test("With error", async () => {
        mockedClient.uploadPart.mockImplementation(() => Promise.reject("error-reason"));
        await addAttachment(context, actionParams);
        expect(mockedClient.uploadPart).toHaveBeenCalled();
        expect(context.commit).toHaveBeenNthCalledWith(1, ADD_FILE, expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(2, ADD_ATTACHMENT, expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(4, SET_MESSAGE_HAS_ATTACHMENT, {
            key: message.key,
            hasAttachment: true
        });
        expect(context.commit).toHaveBeenNthCalledWith(
            5,
            SET_FILE_PROGRESS,
            expect.objectContaining({
                loaded: 100,
                total: 100
            })
        );
        expect(context.commit).toHaveBeenNthCalledWith(
            6,
            SET_FILE_STATUS,
            expect.objectContaining({ status: FileStatus.ERROR })
        );
    });

    test("Cancelled by user", async () => {
        mockedClient.uploadPart.mockImplementation(() => Promise.reject({ message: "CANCELLED_BY_CLIENT" }));
        jest.useFakeTimers();
        await addAttachment(context, actionParams);
        expect(mockedClient.uploadPart).toHaveBeenCalledWith(expect.anything(), expect.anything(), expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(1, ADD_FILE, expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(2, ADD_ATTACHMENT, expect.anything());
        jest.runAllTimers();
        expect(context.commit).toHaveBeenNthCalledWith(5, REMOVE_ATTACHMENT, expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(6, REMOVE_FILE, expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(
            7,
            SET_MESSAGE_HAS_ATTACHMENT,
            expect.objectContaining({ hasAttachment: false })
        );
    });
});
