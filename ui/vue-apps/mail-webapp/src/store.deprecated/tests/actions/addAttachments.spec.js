import { addAttachments } from "../../actions/addAttachments";
import { MockMailboxItemsClient } from "@bluemind/test-utils";
import ServiceLocator from "@bluemind/inject";

const context = {
    commit: jest.fn(),
    dispatch: jest.fn().mockReturnValue(Promise.resolve()),
    state: { currentMessage: { parts: { attachments: [] } } },
    rootGetters: { "mail/MY_DRAFTS": { key: "draft-key", uid: "trash-uid" } }
};

const mockedClient = new MockMailboxItemsClient();
ServiceLocator.register({ provide: "MailboxItemsPersistence", factory: () => mockedClient });
const files = [
    new Blob(["myfilecontentasastring"], {
        size: 22,
        type: "text/plain"
    })
];
files.name = "TestFile.txt";

describe("[Mail-WebappStore][actions] : addAttachments", () => {
    beforeEach(() => {
        context.commit.mockClear();
        context.dispatch.mockClear();
    });
    test("Attach text file", async () => {
        await addAttachments(context, files);
        expect(mockedClient.uploadPart).toHaveBeenCalledWith(expect.anything(), expect.anything(), expect.anything());
        expect(context.commit).toHaveBeenCalledWith("draft/addAttachment", expect.anything());
    });
    test("With error", async () => {
        mockedClient.uploadPart.mockImplementation(() => Promise.reject("error-reason"));
        await addAttachments(context, files);
        expect(mockedClient.uploadPart).toHaveBeenCalledWith(expect.anything(), expect.anything(), expect.anything());
        expect(context.commit).toHaveBeenCalledWith("draft/addAttachment", expect.anything());
        expect(context.commit).toHaveBeenCalledWith("draft/setAttachmentProgress", expect.anything());
        expect(context.commit).toHaveBeenCalledWith("draft/setAttachmentStatus", expect.anything());
    });
    test("Cancelled", async () => {
        mockedClient.uploadPart.mockImplementation(() => Promise.reject({ message: "CANCELLED_BY_CLIENT" }));
        jest.useFakeTimers();
        await addAttachments(context, files);
        expect(mockedClient.uploadPart).toHaveBeenCalledWith(expect.anything(), expect.anything(), expect.anything());
        expect(context.commit).toHaveBeenCalledWith("draft/addAttachment", expect.anything());
        expect(context.commit).toHaveBeenCalledWith("draft/setAttachmentProgress", expect.anything());
        jest.runAllTimers();
        expect(context.commit).toHaveBeenCalledWith("draft/removeAttachment", expect.anything());
    });
});
