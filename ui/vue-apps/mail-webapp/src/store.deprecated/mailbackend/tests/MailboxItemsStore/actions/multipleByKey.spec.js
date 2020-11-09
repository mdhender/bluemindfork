import { multipleByKey } from "../../../MailboxItemsStore/actions/multipleByKey";
import { ItemUri } from "@bluemind/item-uri";
import { FETCH_MESSAGE_METADATA } from "~actions";

const context = {
    dispatch: jest.fn(),
    rootState: {
        mail: {
            folders: []
        }
    }
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
        context.dispatch.mockClear();
    });
    test("call multipleById service for each folder mutate state with result", () => {
        const keys = generateKeys(10, 20, "container1").concat(
            generateKeys(10, 20, "container2"),
            generateKeys(20, 30, "container3")
        );
        multipleByKey(context, keys);
        expect(context.dispatch).toHaveBeenCalledWith(
            "mail/" + FETCH_MESSAGE_METADATA,
            {
                folders: [],
                messageKeys: keys
            },
            { root: true }
        );
    });
    test("fail if multipleById call fail", async () => {
        const messageKey = ItemUri.encode("itemId", "folderUid");
        context.dispatch.mockReturnValueOnce(Promise.reject("Error!"));
        await expect(multipleByKey(context, [messageKey])).rejects.toBe("Error!");
    });
});
