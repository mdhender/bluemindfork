import { addAttachment } from "../../src/actions/addAttachment";
import { MockMailboxItemsClient } from "@bluemind/test-mocks";
import ServiceLocator from "@bluemind/inject";

const context = {
    commit: jest.fn(),
    dispatch: jest.fn().mockReturnValue(Promise.resolve()),
    getters: {
        my: {
            DRAFTS: { key: "draft-key", uid: "trash-uid" }
        }
    },
    state: { currentMessage: { parts: { attachments: [] } } }
};

const mockedClient = new MockMailboxItemsClient();
ServiceLocator.register({ provide: "MailboxItemsPersistence", factory: () => mockedClient });
const file = new Blob(["myfilecontentasastring"], {
    size: 22,
    type: "text/plain"
});
file.name = "TestFile.txt";

describe("[Mail-WebappStore][actions] : addAttachment", () => {
    beforeEach(() => {
        context.commit.mockClear();
        context.dispatch.mockClear();
    });
    test("Attach text file", async () => {
        await addAttachment(context, file);
        expect(mockedClient.uploadPart).toHaveBeenCalledWith(expect.anything());
        expect(context.commit).toHaveBeenCalledWith("draft/addAttachment", expect.anything());
    });
    test("With error", async () => {
        mockedClient.uploadPart.mockImplementation(() => Promise.reject("error-reason"));
        await addAttachment(context, file);
        expect(mockedClient.uploadPart).toHaveBeenCalledWith(expect.anything());
        expect(context.commit).toHaveBeenCalledWith("draft/addAttachment", expect.anything());
        expect(context.commit).toHaveBeenCalledWith(
            "alert/add",
            {
                code: "MSG_DRAFT_ATTACH_ERROR",
                props: { filename: "TestFile.txt", reason: "error-reason" }
            },
            { root: true }
        );
    });
});
