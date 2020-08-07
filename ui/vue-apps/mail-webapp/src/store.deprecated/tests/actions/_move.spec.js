import { $_move } from "../../actions/$_move";
import ItemUri from "@bluemind/item-uri";
import ServiceLocator from "@bluemind/inject";
import { MailboxFoldersClient } from "@bluemind/backend.mail.api";

jest.mock("@bluemind/inject");
jest.mock("@bluemind/backend.mail.api");

const sourceFolderKey = "source_key",
    messageKey = ItemUri.encode("message_id", sourceFolderKey),
    destinationKey = "destination_key",
    mailboxUid = "my_mailbox_uid";

const context = {
    commit: jest.fn(),
    dispatch: jest.fn(),
    rootState: {
        mail: {
            folders: {
                [sourceFolderKey]: {
                    mailbox: mailboxUid,
                    id: 20
                },
                [destinationKey]: {
                    mailbox: mailboxUid,
                    id: 10
                }
            }
        }
    }
};

const service = new MailboxFoldersClient();
service.importItems.mockReturnValue(Promise.resolve());
const get = jest.fn().mockReturnValue(service);
ServiceLocator.getProvider.mockReturnValue({
    get
});
describe("[Mail-WebappStore][actions] : $_move", () => {
    beforeEach(() => {
        context.commit.mockClear = {};
        service.importItems.mockClear();
    });

    test("call service to move message", () => {
        $_move(context, { messageKeys: [messageKey], destinationKey });
        expect(get).toHaveBeenCalledWith("my_mailbox_uid");
        expect(service.importItems).toHaveBeenCalledWith(10, {
            mailboxFolderId: 20,
            ids: [{ id: "message_id" }],
            expectedIds: undefined,
            deleteFromSource: true
        });
    });

    test("remove moved message from the state", done => {
        $_move(context, { messageKeys: [messageKey], destinationKey }).then(() => {
            expect(context.dispatch).toHaveBeenCalledWith("_removeMessages", [messageKey]);
            done();
        });
    });
});
