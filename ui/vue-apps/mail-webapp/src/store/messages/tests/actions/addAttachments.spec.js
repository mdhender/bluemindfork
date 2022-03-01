import ServiceLocator from "@bluemind/inject";
import { MockI18NProvider, MockMailboxItemsClient } from "@bluemind/test-utils";

import addAttachments from "../../actions/addAttachments";
import { AttachmentStatus } from "~/model/attachment";
import {
    ADD_ATTACHMENT,
    REMOVE_ATTACHMENT,
    SET_ATTACHMENT_ADDRESS,
    SET_ATTACHMENT_PROGRESS,
    SET_ATTACHMENT_STATUS,
    SET_MESSAGE_HAS_ATTACHMENT
} from "~/mutations";

describe("addAttachments action", () => {
    global.URL.createObjectURL = jest.fn();
    const draft = { key: "blabla", folderRef: { uid: "folder-uid" }, attachments: [] };
    const files = [
        new Blob(["myfilecontentasastring"], {
            size: 22,
            type: "text/plain"
        })
    ];
    files.name = "TestFile.txt";

    const actionParams = { draft, files, messageCompose: {} };
    let context, mockedClient;

    beforeEach(() => {
        context = {
            commit: jest.fn(),
            dispatch: jest.fn().mockReturnValue(Promise.resolve())
        };

        mockedClient = new MockMailboxItemsClient();
        ServiceLocator.register({ provide: "MailboxItemsPersistence", factory: () => mockedClient });
        ServiceLocator.register({ provide: "i18n", factory: () => MockI18NProvider });
    });

    test("Attach text file", async () => {
        await addAttachments(context, actionParams);
        expect(mockedClient.uploadPart).toHaveBeenCalled();
        expect(context.commit).toHaveBeenNthCalledWith(1, ADD_ATTACHMENT, expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(2, SET_MESSAGE_HAS_ATTACHMENT, {
            key: draft.key,
            hasAttachment: true
        });
        expect(context.commit).toHaveBeenNthCalledWith(3, SET_ATTACHMENT_ADDRESS, expect.anything());
    });

    test("With error", async () => {
        mockedClient.uploadPart.mockImplementation(() => Promise.reject("error-reason"));
        await addAttachments(context, actionParams);
        expect(mockedClient.uploadPart).toHaveBeenCalled();
        expect(context.commit).toHaveBeenNthCalledWith(1, ADD_ATTACHMENT, expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(2, SET_MESSAGE_HAS_ATTACHMENT, {
            key: draft.key,
            hasAttachment: true
        });
        expect(context.commit).toHaveBeenNthCalledWith(
            3,
            SET_ATTACHMENT_PROGRESS,
            expect.objectContaining({ messageKey: draft.key, loaded: 100, total: 100 })
        );
        expect(context.commit).toHaveBeenNthCalledWith(
            4,
            SET_ATTACHMENT_STATUS,
            expect.objectContaining({ messageKey: draft.key, status: AttachmentStatus.ERROR })
        );
    });

    test("Cancelled by user", async () => {
        mockedClient.uploadPart.mockImplementation(() => Promise.reject({ message: "CANCELLED_BY_CLIENT" }));
        jest.useFakeTimers();
        await addAttachments(context, actionParams);
        expect(mockedClient.uploadPart).toHaveBeenCalledWith(expect.anything(), expect.anything(), expect.anything());
        expect(context.commit).toHaveBeenNthCalledWith(1, ADD_ATTACHMENT, expect.anything());
        jest.runAllTimers();
        expect(context.commit).toHaveBeenNthCalledWith(3, REMOVE_ATTACHMENT, expect.anything());
    });
});
