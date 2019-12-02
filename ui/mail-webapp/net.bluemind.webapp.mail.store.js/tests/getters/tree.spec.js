import { tree } from "../../src/getters/tree";

function generateTree(folders) {
    return folders.map(f => {
        return {
            uid: f,
            key: f,
            value: {
                parentUid: f.replace(/.?[^.]+$/, ""),
                name: f
            }
        };
    });
}

describe("[Mail-WebappStore][getters] : tree ", () => {
    const getters = {};
    const state = {};
    beforeEach(() => {
        getters["folders/folders"] = generateTree(["1", "2", "1.1", "2.1", "2.1.1"]);
        state.foldersData = { "1": { expand: true } };
    });
    test("return tree view of folders", () => {
        const result = tree(state, getters);
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
        getters["folders/folders"] = generateTree([
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
        ]);
        const result = tree(state, getters);
        expect(result[0].uid).toBe("INBOX");
        expect(result[1].uid).toBe("Sent");
        expect(result[2].uid).toBe("Trash");
        expect(result[3].uid).toBe("Junk");
        expect(result[4].uid).toBe("Outbox");
        expect(result[5].uid).toBe("1");
        expect(result[5].children[0].uid).toBe("1.1");
    });
});

// export function tree(state, getters) {
//     const nodeMap = new Map();
//     getters["folders/folders"].forEach(folderItem => {
//         const data = state.foldersData[folderItem.uid] || {};
//         const folder = toTreeItem(folderItem, data);
//         const siblings = nodeMap.has(folder.parent) ? nodeMap.get(folder.parent) : [];
//         const children = nodeMap.has(folder.uid) ? nodeMap.get(folder.uid) : [];
//         siblings.push(folder);
//         siblings.sort(compare);
//         children.sort(compare);
//         folder.children = children;
//         nodeMap.set(folder.parent, siblings);
//         nodeMap.set(folder.uid, children);
//     });
//     return nodeMap.get(null) || [];
// }

// function toTreeItem(folder, { unread, expanded }) {
//     return {
//         uid: folder.uid,
//         key: folder.key,
//         name: folder.value.name,
//         fullname: folder.value.fullName,
//         parent: folder.value.parentUid || null,
//         unread: unread || 0,
//         expanded: !!expanded,
//         children: []
//     };
// }

// const defaultFolders = ["INBOX", "Sent", "Drafts", "Trash", "Junk", "Outbox"];

// function compare(f1, f2) {
//     const f1Weight = defaultFolders.indexOf(f1.name);
//     const f2Weight = defaultFolders.indexOf(f2.name);
//     if (f1Weight >= 0 && f2Weight >= 0) {
//         return f1Weight - f2Weight;
//     } else if (f1Weight >= 0 && f2Weight < 0) {
//         return -1;
//     } else if (f1Weight < 0 && f2Weight >= 0) {
//         return 1;
//     } else {
//         return f1.name.localeCompare(f2.name);
//     }
// }
