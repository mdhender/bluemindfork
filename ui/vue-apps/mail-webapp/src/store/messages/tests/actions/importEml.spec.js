import PostalMime from "postal-mime";
import { ADD_LOCAL_ATTACHMENT } from "~/actions";
import { ADD_MESSAGES, SET_PART_DATA } from "~/mutations";
import importEml from "../../actions/importEml";
import parsed from "./postalMimeResult.json";

global.fetch = jest.fn().mockReturnValue({ blob: () => new Blob(["fakeBlob"]) });
jest.mock("postal-mime", () => jest.fn());
PostalMime.mockImplementation(() => ({ parse: () => parsed }));

describe("importEml action", () => {
    test("Import EML", async () => {
        const store = { commit: jest.fn(), dispatch: jest.fn() };
        await importEml(store, "http://127.0.0.1/fake");
        expect(store.commit).toHaveBeenNthCalledWith(1, ADD_MESSAGES, expect.anything());
        expect(store.dispatch).toHaveBeenNthCalledWith(1, ADD_LOCAL_ATTACHMENT, expect.anything());
        expect(store.dispatch).toHaveBeenNthCalledWith(2, ADD_LOCAL_ATTACHMENT, expect.anything());
        expect(store.dispatch).toHaveBeenNthCalledWith(3, ADD_LOCAL_ATTACHMENT, expect.anything());
        expect(store.commit).toHaveBeenNthCalledWith(2, SET_PART_DATA, expect.anything());
        expect(store.commit).toHaveBeenNthCalledWith(3, SET_PART_DATA, expect.anything());
        expect(store.commit).toHaveBeenNthCalledWith(4, SET_PART_DATA, expect.anything());
        expect(store.commit).toHaveBeenNthCalledWith(5, SET_PART_DATA, expect.anything());
    });
});
