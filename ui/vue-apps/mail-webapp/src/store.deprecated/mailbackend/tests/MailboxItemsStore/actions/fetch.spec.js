import { fetch } from "../../../src/MailboxItemsStore/actions/fetch";
import { ItemUri } from "../../../../../commons/net.bluemind.item-uri.js/src/ItemUri";
import { MockMailboxItemsClient } from "@bluemind/test-mocks";
import ServiceLocator from "@bluemind/inject";

const messageKey = ItemUri.encode("itemId", "folderUid");
const partTextContent = "content";
const part = {
    encoding: "7bit",
    address: "1.1",
    mime: "text/plain",
    charset: "utf-8"
};

const mockedClient = new MockMailboxItemsClient();
mockedClient.mockFetch(partTextContent);
const get = jest.fn().mockReturnValue(mockedClient);
ServiceLocator.register({ provide: "MailboxItemsPersistence", factory: uid => get(uid) });

const context = mockVuexContext();

describe("[MailItemsStore][actions] : fetch", () => {
    beforeEach(() => {
        context.commit.mockClear();
    });

    test("call fetch service for an attachment with a given messageKey and mutate state with result", done => {
        fetch(context, { messageKey, part, isAttachment: true }).then(() => {
            expect(context.commit).toHaveBeenCalledWith("storePartContent", {
                messageKey,
                address: part.address,
                content: new Blob([partTextContent], { type: "text/plain" })
            });
            done();
        });
        checkFetchCall();
    });

    test("call fetch service with a given messageKey and mutate state with result", done => {
        fetch(context, { messageKey, part, isAttachment: false }).then(() => {
            expect(context.commit).toHaveBeenCalledWith("storePartContent", {
                messageKey,
                address: part.address,
                content: partTextContent
            });
            done();
        });
        checkFetchCall();
    });

    test("fail if sortedIds call fail", async () => {
        ServiceLocator.getProvider("MailboxItemsPersistence")
            .get()
            .fetch.mockReturnValueOnce(Promise.reject("Error!"));
        await expect(fetch(context, { messageKey, part, isAttachment: false })).rejects.toBe("Error!");
    });

    function checkFetchCall() {
        expect(get).toHaveBeenCalledWith("folderUid");
        expect(ServiceLocator.getProvider("MailboxItemsPersistence").get().fetch).toHaveBeenCalledWith(
            context.state.items[messageKey].value.imapUid,
            part.address,
            part.encoding,
            part.mime,
            part.charset
        );
    }
});

function mockVuexContext() {
    return {
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
}
