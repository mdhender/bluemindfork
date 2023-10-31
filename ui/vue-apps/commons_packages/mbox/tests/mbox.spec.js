import { extractFolderUid } from "../src/";

describe("Mbox tests", () => {
    test("extractFolderUid", () => {
        expect(extractFolderUid("mbox_records_xxxx-xxxx-xxxx-xxxx")).toBe("xxxx-xxxx-xxxx-xxxx");
        expect(extractFolderUid("xxxx-xxxx-xxxx-xxxx")).toBe("xxxx-xxxx-xxxx-xxxx");
    });
});
