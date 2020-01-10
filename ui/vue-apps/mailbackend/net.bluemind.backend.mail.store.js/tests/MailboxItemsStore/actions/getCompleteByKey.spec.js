import { getCompleteByKey } from "../../../src/MailboxItemsStore/actions/getCompleteByKey";
import ServiceLocator from "@bluemind/inject";
import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import { ItemUri } from "../../../../../commons/net.bluemind.item-uri.js/src/ItemUri";
jest.mock("@bluemind/inject");
jest.mock("@bluemind/backend.mail.api");

const service = new MailboxItemsClient();
service.getCompleteById.mockReturnValue(Promise.resolve("item"));
const get = jest.fn().mockReturnValue(service);
ServiceLocator.getProvider.mockReturnValue({
    get
});

const messageKey = ItemUri.encode("itemId", "folderUid");
const context = {
    commit: jest.fn()
};

describe("[MailItemsStore][actions] : getCompleteByKey", () => {
    beforeEach(() => {
        context.commit.mockClear();
    });
    test("call getCompleteById service for the given folder and mutate state with result", done => {
        getCompleteByKey(context, messageKey).then(() => {
            expect(context.commit).toHaveBeenCalledWith("storeItems", { items: ["item"], folderUid: "folderUid" });
            done();
        });
        expect(get).toHaveBeenCalledWith("folderUid");
        expect(service.getCompleteById).toHaveBeenCalledWith("itemId");
    });
    test("fail if sortedIds call fail", () => {
        service.getCompleteById.mockReturnValueOnce(Promise.reject("Error!"));
        expect(getCompleteByKey(context, messageKey)).rejects.toBe("Error!");
    });
});
