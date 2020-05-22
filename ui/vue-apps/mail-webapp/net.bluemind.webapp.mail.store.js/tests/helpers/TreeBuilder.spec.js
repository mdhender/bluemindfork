import { TreeBuilder } from "../../src/getters/helpers/TreeBuilder";

function generateTree(folders) {
    return folders.map(f => {
        return {
            uid: f,
            key: f + "-key",
            value: {
                parentUid: f.replace(/.?[^.]+$/, ""),
                name: f
            }
        };
    });
}

describe("[Mail-WebappStore][utils] : TreeBuilder ", () => {
    test("transform folder into TreeNodes", () => {
        const folder = { uid: "uid", key: "key", value: { parentUid: "parent", name: "name", fullName: "full/name" } };
        expect(TreeBuilder.toTreeItem(folder, { unread: 3, expanded: true })).toEqual({
            uid: "uid",
            key: "key",
            name: "name",
            fullName: "full/name",
            parent: "parent",
            unread: 3,
            expanded: true,
            loaded: true,
            children: [],
            editing: false
        });
    });

    test("return tree view of nodes", () => {
        const nodes = generateTree(["1", "2", "1.1", "2.1", "2.1.1"]).map(TreeBuilder.toTreeItem);
        const result = TreeBuilder.build(nodes);
        expect(result.length).toBe(2);
        expect(result[0].uid).toEqual("1");
        expect(result[1].uid).toEqual("2");
        expect(result[0].children.length).toBe(1);
        expect(result[0].children[0].uid).toEqual("1.1");
        expect(result[0].children[0].children.length).toBe(0);
        expect(result[1].children.length).toBe(1);
        expect(result[1].children[0].uid).toEqual("2.1");
        expect(result[1].children[0].children.length).toBe(1);
        expect(result[1].children[0].children[0].uid).toBe("2.1.1");
    });
    test("sort folders according to name / default order", () => {
        const nodes = generateTree([
            "2",
            "Junk",
            "1",
            "Outbox",
            "1.2",
            "Sent",
            "1.1",
            "Trash",
            "2.1",
            "2.1.1",
            "INBOX"
        ]).map(TreeBuilder.toTreeItem);
        const result = TreeBuilder.build(nodes);
        expect(result[0].uid).toBe("INBOX");
        expect(result[1].uid).toBe("Sent");
        expect(result[2].uid).toBe("Trash");
        expect(result[3].uid).toBe("Junk");
        expect(result[4].uid).toBe("Outbox");
        expect(result[5].uid).toBe("1");
        expect(result[5].children[0].uid).toBe("1.1");
    });
});
