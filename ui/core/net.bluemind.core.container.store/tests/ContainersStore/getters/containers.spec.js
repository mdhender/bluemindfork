import { containers as containersGetter } from "../../../src/ContainersStore/getters/containers";

const containers = {
    key1: { key: "key1", uid: "a", value: "value" },
    key2: { key: "key2", uid: "b", value: "value" },
    key3: { key: "key3", uid: "c", value: "value" },
    key4: { key: "key4", uid: "a.d", value: "value" },
    key5: { key: "key5", uid: "a.e", value: "value" },
    key6: { key: "key6", uid: "b.f", value: "value" },
    key7: { key: "key7", uid: "b.g", value: "value" },
    key8: { key: "key8", uid: "b.h", value: "value" },
    key9: { key: "key9", uid: "b.h.i", value: "value" },
    key10: { key: "key10", uid: "b.h.j", value: "value" },
    key11: { key: "key11", uid: "b.h.j.k", value: "value" },
    key12: { key: "key12", uid: "a.e.l", value: "value" }
};
const containerKeys = ["key1", "key4", "key5", "key12", "key2", "key6", "key7", "key8", "key9", "key10", "key11"];
const state = {
    containers,
    containerKeys
};

describe("[ContainersStore][getters] : containers ", () => {
    test("returned value matches container's keys order", () => {
        const result = containersGetter(state);
        result.forEach((f, i) => expect(f.key).toEqual(containerKeys[i]));
    });
    test("does not contain a container that is not in containersKeys", () => {
        const result = containersGetter(state);
        expect(result).toEqual(expect.not.arrayContaining([containers.key3]));
    });
});
