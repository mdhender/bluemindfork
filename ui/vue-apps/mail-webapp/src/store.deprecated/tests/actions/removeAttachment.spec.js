import { removeAttachment } from "../../actions/removeAttachment";
import { MockMailboxItemsClient } from "@bluemind/test-utils";
import ServiceLocator from "@bluemind/inject";

const attachmentUid = "uid1",
    attachmentAddress = "myAddress";

const context = {
    commit: jest.fn(),
    dispatch: jest.fn().mockReturnValue(Promise.resolve()),
    getters: {
        "draft/getAttachmentStatus": jest.fn()
    },
    state: {
        draft: {
            parts: {
                attachments: [
                    {
                        uid: attachmentUid,
                        address: attachmentAddress
                    }
                ]
            }
        }
    },
    rootGetters: {
        "mail/MY_DRAFTS": jest.fn().mockReturnValue({})
    }
};

let mockedClient;

describe("[Mail-WebappStore][actions] : removeAttachment", () => {
    beforeEach(() => {
        mockedClient = new MockMailboxItemsClient();
        ServiceLocator.register({ provide: "MailboxItemsPersistence", factory: () => mockedClient });
    });

    test("Basic remove of an attachment", async () => {
        await removeAttachment(context, attachmentUid);
        expect(mockedClient.removePart).toHaveBeenCalledWith(attachmentAddress);
        expect(context.commit).toHaveBeenCalledWith("draft/removeAttachment", attachmentUid);
        expect(context.dispatch).toHaveBeenCalledWith("saveDraft");
    });

    test("Remove of an attachment in error", async () => {
        context.getters["draft/getAttachmentStatus"] = jest.fn().mockReturnValue("ERROR");
        await removeAttachment(context, attachmentUid);
        expect(mockedClient.removePart).not.toHaveBeenCalled();
        expect(context.commit).toHaveBeenCalledWith("draft/removeAttachment", attachmentUid);
        expect(context.dispatch).toHaveBeenCalledWith("saveDraft");
    });
});
