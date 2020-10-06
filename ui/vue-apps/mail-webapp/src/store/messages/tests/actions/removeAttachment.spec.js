import ServiceLocator from "@bluemind/inject";
import { MockMailboxItemsClient } from "@bluemind/test-utils";

import removeAttachment from "../../actions/removeAttachment";
import mutationTypes from "../../../mutationTypes";
import actionTypes from "../../../actionTypes";
import { AttachmentStatus } from "../../../../model/attachment";

describe("removeAttachment action", () => {
    let mockedClient, context;
    const address = "2.3";
    const messageKey = "blabla";
    const draftFolderKey = "draf:uid";
    const actionParams = {
        messageKey,
        attachmentAddress: address,
        userPrefTextOnly: true,
        myDraftsFolderKey: draftFolderKey,
        messageCompose: {}
    };

    beforeEach(() => {
        context = {
            commit: jest.fn(),
            dispatch: jest.fn().mockReturnValue(Promise.resolve()),
            rootGetters: {
                "mail/MY_DRAFTS": { remoteRef: { uid: draftFolderKey } }
            },
            state: {
                [messageKey]: {
                    folderRef: { uid: draftFolderKey },
                    attachments: [{ address }]
                }
            }
        };
        mockedClient = new MockMailboxItemsClient();
        ServiceLocator.register({ provide: "MailboxItemsPersistence", factory: () => mockedClient });
    });

    test("Basic remove of an attachment", async () => {
        await removeAttachment(context, actionParams);
        expect(mockedClient.removePart).toHaveBeenCalledWith(address);
        expect(context.commit).toHaveBeenCalledWith(mutationTypes.REMOVE_ATTACHMENT, { messageKey, address });
        expect(context.dispatch).toHaveBeenCalledWith(actionTypes.SAVE_MESSAGE, expect.anything());
    });

    test("Remove of an attachment in error", async () => {
        context.state[messageKey].attachments[0].status = AttachmentStatus.ERROR;
        await removeAttachment(context, actionParams);
        expect(mockedClient.removePart).not.toHaveBeenCalled();
        expect(context.commit).toHaveBeenCalledWith(mutationTypes.REMOVE_ATTACHMENT, { messageKey, address });
        expect(context.dispatch).toHaveBeenCalledWith(actionTypes.SAVE_MESSAGE, expect.anything());
    });
});
