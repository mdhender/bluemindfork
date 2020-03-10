import { multipleByKey } from "../../../src/MailboxItemsStore/actions/multipleByKey";
import ServiceLocator from "@bluemind/inject";
import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import { ItemUri } from "@bluemind/item-uri/src/ItemUri";
jest.mock("@bluemind/inject");
jest.mock("@bluemind/backend.mail.api");

const service = new MailboxItemsClient();
service.multipleById.mockReturnValue(Promise.resolve("items"));
const get = jest.fn().mockReturnValue(service);
ServiceLocator.getProvider.mockReturnValue({
    get
});
const context = {
    commit: jest.fn()
};

function generateKeys(start, end, folderUid) {
    const keys = [];
    for (let i = start; i < end; i++) {
        keys.push(ItemUri.encode(i, folderUid));
    }
    return keys;
}

describe("[MailItemsStore][actions] : multipleByKey", () => {
    beforeEach(() => {
        context.commit.mockClear();
        MailboxItemsClient.mockClear();
        get.mockClear();
    });
    test("call multipleById service for each folder mutate state with result", done => {
        const messages = {
            container1: new Array(10).fill(null).map((v, i) => i + 10),
            container2: new Array(10).fill(null).map((v, i) => i + 10),
            container3: new Array(10).fill(null).map((v, i) => i + 20)
        };
        const keys = generateKeys(10, 20, "container1").concat(
            generateKeys(10, 20, "container2"),
            generateKeys(20, 30, "container3")
        );
        multipleByKey(context, keys).then(() => {
            expect(context.commit).toHaveBeenCalledTimes(3);
            expect(context.commit).toHaveBeenCalledWith("storeItems", { items: "items", folderUid: "container1" });
            expect(context.commit).toHaveBeenCalledWith("storeItems", { items: "items", folderUid: "container2" });
            expect(context.commit).toHaveBeenCalledWith("storeItems", { items: "items", folderUid: "container3" });
            done();
        });
        expect(get).toHaveBeenCalledTimes(3);
        expect(get).toHaveBeenCalledWith("container1");
        expect(get).toHaveBeenCalledWith("container2");
        expect(get).toHaveBeenCalledWith("container3");
        expect(service.multipleById).toHaveBeenCalledTimes(3);
        expect(service.multipleById).toHaveBeenCalledWith(messages.container1);
        expect(service.multipleById).toHaveBeenCalledWith(messages.container2);
        expect(service.multipleById).toHaveBeenCalledWith(messages.container3);
    });
    test("fail if multipleById call fail", () => {
        const messageKey = ItemUri.encode("itemId", "folderUid");
        service.multipleById.mockReturnValueOnce(Promise.reject("Error!"));
        expect(multipleByKey(context, [messageKey])).rejects.toBe("Error!");
    });
});
