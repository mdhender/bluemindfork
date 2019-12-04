import ContainerStore from "../../src/ContainersStore";
import ServiceLocator from "@bluemind/inject";
import { ContainersClient } from "@bluemind/core.container.api";
import containers from "./data/containers";
import { createLocalVue } from "@vue/test-utils";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";

jest.mock("@bluemind/inject");
jest.mock("@bluemind/core.container.api");

const service = new ContainersClient();
const get = jest.fn().mockReturnValue(service);

ServiceLocator.getProvider.mockReturnValue({
    get
});

const localVue = createLocalVue();
localVue.use(Vuex);

service.all.mockImplementation(({ type, verb }) =>
    Promise.resolve(containers.filter(c => c.type == type && verb.some(v => c.verbs.includes(v))))
);
describe("[ContainerStore] Vuex store", () => {
    test("can load all containers into store", done => {
        const store = new Vuex.Store(cloneDeep(ContainerStore));
        store.dispatch("all", { type: "mailboxacl", verb: ["Read"] }).then(() => {
            const uids = [
                "mailbox:acls-6793466E-F5D4-490F-97BF-DF09D3327BF4",
                "mailbox:acls-AB6A2A90-04DA-4BD8-8E56-C4A11666E6CC",
                "mailbox:acls-A78219B7-6F50-457D-BECA-4614865B3E2B"
            ];
            expect(store.getters.containers).toEqual(containers.filter(container => uids.includes(container.uid)));
            done();
        });
    });
});
