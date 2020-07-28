import folders1 from "./data/folders-1";
import folders2 from "./data/folders-2";
import MailboxFolderStore from "../../src/MailboxFoldersStore";
import Folder from "../../src/MailboxFoldersStore/Folder";
import ServiceLocator from "@bluemind/inject";
import { MailboxFoldersClient } from "@bluemind/backend.mail.api";
import ItemUri from "@bluemind/item-uri";
import { createLocalVue } from "@vue/test-utils";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";

jest.mock("@bluemind/inject");
jest.mock("@bluemind/backend.mail.api");

const service = new MailboxFoldersClient();
const get = jest.fn().mockReturnValue(service);
ServiceLocator.getProvider.mockReturnValue({
    get
});

const localVue = createLocalVue();
localVue.use(Vuex);

describe("[MailboxFolderStore] Vuex store", () => {
    test("can load folders from a mailbox into store", done => {
        const mailboxUid1 = "mailbox:1:uid";
        const mailboxUid2 = "mailbox:2:uid";
        service.all.mockReturnValueOnce(Promise.resolve(folders1));
        service.all.mockReturnValueOnce(Promise.resolve(folders2));
        const store = new Vuex.Store(cloneDeep(MailboxFolderStore));
        store
            .dispatch("all", mailboxUid1)
            .then(store.dispatch("all", mailboxUid2))
            .then(() => {
                expect(store.state.itemKeys.length).toEqual(folders1.length + folders2.length);

                folders1.forEach(item => {
                    const key = ItemUri.encode(item.uid, mailboxUid1);
                    const folder = new Folder(key, item);
                    expect(store.getters.getFolderByKey(key)).toEqual(folder);
                });
                let defaults = store.getters.getDefaultFolders(mailboxUid1);
                expect(defaults.INBOX).toBe(store.getters.getFolderByPath("INBOX", mailboxUid1));
                expect(defaults.INBOX.uid).toEqual("f1c3f42f-551b-446d-9682-cfe0574b3205");
                expect(defaults.TRASH).toBe(store.getters.getFolderByPath("Trash", mailboxUid1));
                let folder = store.getters.getFolderByKey(
                    ItemUri.encode("65b51768-8544-4bb3-9950-c6f4fc32a52e", mailboxUid1)
                );
                expect(folder).toBe(store.getters.getFolderByPath("Archives/2017/Clients", mailboxUid1));

                folders2.forEach(item => {
                    const key = ItemUri.encode(item.uid, mailboxUid2);
                    const folder = new Folder(key, item);
                    expect(store.getters.getFolderByKey(key)).toEqual(folder);
                });
                defaults = store.getters.getDefaultFolders(mailboxUid2);
                expect(defaults.INBOX).toBe(store.getters.getFolderByPath("INBOX", mailboxUid2));
                expect(defaults.INBOX.uid).toEqual("25252a1d-8e3b-4252-971a-bc3b05d99ca8");
                expect(defaults.TRASH).toBe(store.getters.getFolderByPath("Trash", mailboxUid2));
                folder = store.getters.getFolderByKey(
                    ItemUri.encode("f51ee0c7-c443-448f-842a-1682f8605a8d", mailboxUid2)
                );
                expect(folder).toBe(store.getters.getFolderByPath("/Roadmap/4.X/Nouveau Webmail", mailboxUid2));
                done();
            });
    });
    test("can create folders", done => {
        const mailboxUid = "mailbox:uid";
        const roadmap = folders2[0];
        const roadmapKey = ItemUri.encode(roadmap.uid, mailboxUid);
        const four = folders2[3];
        const fourKey = ItemUri.encode(four.uid, mailboxUid);
        service.createBasic.mockReturnValueOnce(Promise.resolve({ uid: roadmap.uid }));
        service.getComplete.mockReturnValueOnce(Promise.resolve(roadmap));
        service.createBasic.mockReturnValueOnce(Promise.resolve({ uid: four.uid }));
        service.getComplete.mockReturnValueOnce(Promise.resolve(four));
        const store = new Vuex.Store(cloneDeep(MailboxFolderStore));
        store
            .dispatch("create", { name: roadmap.value.name, parentUid: null, mailboxUid })
            .then(key => {
                expect(key).toEqual(roadmapKey);
                return store.dispatch("create", { name: four.value.name, parentUid: roadmap.uid, mailboxUid });
            })
            .then(key => {
                expect(key).toEqual(fourKey);
                expect(store.state.itemKeys.length).toEqual(2);
                expect(store.getters.getFolderByPath("/Roadmap", mailboxUid)).toEqual(new Folder(roadmapKey, roadmap));
                expect(store.getters.getFolderByPath("/Roadmap/4", mailboxUid)).toEqual(new Folder(fourKey, four));
                done();
            });
    });
});
