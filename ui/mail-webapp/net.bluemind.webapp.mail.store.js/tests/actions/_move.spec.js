import { $_move } from "../../src/actions/$_move";
import ItemUri from "@bluemind/item-uri";
import ServiceLocator from "@bluemind/inject";
import { MailboxFoldersClient } from "@bluemind/backend.mail.api";

jest.mock("@bluemind/inject");
jest.mock("@bluemind/backend.mail.api");

const context = {
    getters: {
        my: {
            mailboxUid: "my_mailbox_uid"
        },
        "folders/getFolderByKey": jest.fn().mockReturnValue({
            internalId: 1
        })
    },
    commit: jest.fn()
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
        context.getters["folders/getFolderByKey"].mockClear();
    });
    test("call service to move message", () => {
        context.getters["folders/getFolderByKey"].mockReturnValueOnce({ internalId: 10 });
        context.getters["folders/getFolderByKey"].mockReturnValueOnce({ internalId: 20 });
        const messageKey = ItemUri.encode("message_id", "source_uid"),
            destinationKey = ItemUri.encode("destination_uid", "mailbox_uid");

        $_move(context, { messageKey, destinationKey });

        expect(context.getters["folders/getFolderByKey"]).toHaveBeenNthCalledWith(1, destinationKey);
        expect(context.getters["folders/getFolderByKey"]).toHaveBeenNthCalledWith(
            2,
            ItemUri.encode("source_uid", "my_mailbox_uid")
        );
        expect(get).toHaveBeenCalledWith("mailbox_uid");
        expect(service.importItems).toHaveBeenCalledWith(10, {
            mailboxFolderId: 20,
            ids: [{ id: "message_id" }],
            expectedIds: undefined,
            deleteFromSource: true
        });
    });
    test("remove moved message from the state", done => {
        const messageKey = ItemUri.encode("message_id", "source_uid"),
            destinationKey = ItemUri.encode("destination_uid", "mailbox_uid");
        $_move(context, { messageKey, destinationKey }).then(() => {
            expect(context.commit).toHaveBeenCalledWith("messages/removeItems", [messageKey]);
            done();
        });
    });
});
