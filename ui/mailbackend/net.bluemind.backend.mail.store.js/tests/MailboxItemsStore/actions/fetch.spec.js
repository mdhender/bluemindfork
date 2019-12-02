import { fetch } from "../../../src/MailboxItemsStore/actions/fetch";
import ServiceLocator from "@bluemind/inject";
import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import { ItemUri } from "../../../../../commons/net.bluemind.item-uri.js/src/ItemUri";
jest.mock("@bluemind/inject");
jest.mock("@bluemind/backend.mail.api");

const service = new MailboxItemsClient();
service.fetch.mockReturnValue(Promise.resolve("content"));
const get = jest.fn().mockReturnValue(service);
ServiceLocator.getProvider.mockReturnValue({
    get
});

const messageKey = ItemUri.encode("itemId", "folderUid");
const context = {
    commit: jest.fn(),
    state: {
        items: {
            [messageKey]: {
                value: {
                    imapUid: "1."
                }
            }
        }
    }
};
const part = {
    encoding: "7bit",
    address: "1.1",
    mime: "text/plain",
    charset: "utf-8"
};

describe("[MailItemsStore][actions] : fetch", () => {
    beforeEach(() => {
        context.commit.mockClear();
    });
    test("call fetch service for the given folder and mutate state with result", done => {
        fetch(context, { messageKey, part, isAttachment: false }).then(() => {
            expect(context.commit).toHaveBeenCalledWith("storePartContent", {
                messageKey,
                address: part.address,
                content: "content"
            });
            done();
        });
        expect(get).toHaveBeenCalledWith("folderUid");
        expect(service.fetch).toHaveBeenCalledWith(context.state.items[messageKey].value.imapUid, part.address, {
            encoding: part.encoding,
            mime: part.mime,
            charset: part.charset
        });
    });
    test("fail if sortedIds call fail", () => {
        service.fetch.mockReturnValueOnce(Promise.reject("Error!"));
        expect(fetch(context, { messageKey, part, isAttachment: false })).rejects.toBe("Error!");
    });
});
