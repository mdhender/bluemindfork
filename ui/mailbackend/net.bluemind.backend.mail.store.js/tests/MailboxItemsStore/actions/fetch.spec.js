import { fetch } from "../../../src/MailboxItemsStore/actions/fetch";
import ServiceLocator from "@bluemind/inject";
import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import { ItemUri } from "../../../../../commons/net.bluemind.item-uri.js/src/ItemUri";
jest.mock("@bluemind/inject");
jest.mock("@bluemind/backend.mail.api");

const part = {
    encoding: "7bit",
    address: "1.1",
    mime: "text/plain",
    charset: "utf-8"
};
const partTextContent = "content";
const blobPartContent = new Blob([partTextContent], { type : part.mime });

const service = new MailboxItemsClient();
service.fetch.mockImplementation(() => {
    return Promise.resolve(blobPartContent);
});
const get = jest.fn().mockReturnValue(service);
ServiceLocator.getProvider.mockReturnValue({
    get
});

const messageKey = ItemUri.encode("itemId", "folderUid");

const context = mockVuexContext();
mockFileReader();

describe("[MailItemsStore][actions] : fetch", () => {
    beforeEach(() => {
        context.commit.mockClear();
    });

    test("call fetch service for an attachment with a given messageKey and mutate state with result", done => {
        fetch(context, { messageKey, part, isAttachment: true }).then(() => {
            expect(context.commit).toHaveBeenCalledWith("storePartContent", {
                messageKey,
                address: part.address,
                content: blobPartContent
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

    test("fail if sortedIds call fail", () => {
        service.fetch.mockReturnValueOnce(Promise.reject("Error!"));
        expect(fetch(context, { messageKey, part, isAttachment: false })).rejects.toBe("Error!");
    });

    function checkFetchCall() {
        expect(get).toHaveBeenCalledWith("folderUid");
        expect(service.fetch).toHaveBeenCalledWith(
            context.state.items[messageKey].value.imapUid, 
            part.address,
            part.encoding,
            part.mime,
            part.charset
        );
    }
});

function mockFileReader() {
    window.FileReader =  jest.fn(() => ({
        readAsText: jest.fn(),
        addEventListener: jest.fn().mockImplementation((eventType, handler) => {
            if (eventType == "loadend") {
                handler({
                    target: {
                        result: partTextContent
                    }
                });
            }
        })
    }));
}

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