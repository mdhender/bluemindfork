import { tree } from "../../src/getters/tree";
import { TreeBuilder } from "../../src/getters/helpers/TreeBuilder";

jest.mock("../../src/getters/helpers/TreeBuilder");

describe("[Mail-WebappStore][getters] : tree ", () => {
    const getters = { my: {} };
    const state = {};
    beforeEach(() => {
        getters.my.folders = [{ uid: "1" }, { uid: "2" }];
        getters["folders/getFoldersByMailbox"] = uid => [{ uid: uid }, { uid: uid + "-1" }];
        getters.mailshares = [{ mailboxUid: "ms1" }, { mailboxUid: "ms2" }];
        state.foldersData = { "1": { expand: true, unread: 2 }, "ms1-1": { expand: true, unread: 4 } };
        TreeBuilder.toTreeItem.mockClear();
        TreeBuilder.build.mockClear();
        TreeBuilder.build.mockReturnValueOnce("my");
        TreeBuilder.build.mockReturnValueOnce("mailshares");
    });
    test("Use TreeBuilder to build a tree from my folders", () => {
        TreeBuilder.toTreeItem.mockReturnValue("node");
        TreeBuilder.build.mockReturnValueOnce("my");
        TreeBuilder.build.mockReturnValueOnce("mailshares");
        const val = tree(state, getters);
        expect(TreeBuilder.toTreeItem).toHaveBeenCalledWith({ uid: "1" }, { expand: true, unread: 2 });
        expect(TreeBuilder.build).toHaveBeenCalledWith(["node", "node"]);
        expect(val.my).toEqual("my");
    });
    test("Use TreeBuilder to build a tree from mailshares's folders", () => {
        TreeBuilder.toTreeItem.mockReturnValue("node");
        const val = tree(state, getters);
        expect(TreeBuilder.toTreeItem).toHaveBeenCalledWith({ uid: "ms1" }, {});
        expect(TreeBuilder.toTreeItem).toHaveBeenCalledWith({ uid: "ms1-1" }, { expand: true, unread: 4 });
        expect(TreeBuilder.toTreeItem).toHaveBeenCalledWith({ uid: "ms2" }, {});
        expect(TreeBuilder.toTreeItem).toHaveBeenCalledWith({ uid: "ms2-1" }, {});
        expect(TreeBuilder.build).toHaveBeenCalledWith(["node", "node", "node", "node"]);
        expect(val.mailshares).toEqual("mailshares");
    });
});
