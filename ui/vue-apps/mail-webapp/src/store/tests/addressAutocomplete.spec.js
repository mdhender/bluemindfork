import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import { ADDRESS_AUTOCOMPLETE } from "~/getters";
import { ADD_ADDRESS_WEIGHT } from "~/mutations";
import storeData from "../addressAutocomplete";

Vue.use(Vuex);

describe("address autocomplete", () => {
    let store;

    beforeEach(() => {
        store = new Vuex.Store(cloneDeep(storeData));
    });

    test("ADD_ADDRESS_WEIGHT mutation & ADDRESS_AUTOCOMPLETE getter (sorted addresses)", () => {
        const address1 = "address-1";
        expect(store.state.addressWeights[address1]).toBeFalsy();
        store.commit(ADD_ADDRESS_WEIGHT, { address: address1, weight: 1 });
        expect(store.state.addressWeights[address1]).toEqual(1);
        store.commit(ADD_ADDRESS_WEIGHT, { address: address1, weight: 1 });
        expect(store.state.addressWeights[address1]).toEqual(2);
        store.commit(ADD_ADDRESS_WEIGHT, { address: address1, weight: -1 });
        expect(store.state.addressWeights[address1]).toEqual(1);
        store.commit(ADD_ADDRESS_WEIGHT, { address: address1, weight: 1 });

        const address2 = "address-2";
        store.commit(ADD_ADDRESS_WEIGHT, { address: address2, weight: 3 });

        const address3 = "address-3";
        store.commit(ADD_ADDRESS_WEIGHT, { address: address3, weight: -1 });

        expect(store.state.addressWeights).toMatchSnapshot();

        const { sortedAddresses } = store.getters[ADDRESS_AUTOCOMPLETE];
        expect(sortedAddresses[0]).toEqual(address2);
        expect(sortedAddresses[1]).toEqual(address1);
        expect(sortedAddresses[2]).toBeFalsy();
    });

    test("ADDRESS_AUTOCOMPLETE getter (excluded addresses)", () => {
        const address1 = "address-1";
        store.commit(ADD_ADDRESS_WEIGHT, { address: address1, weight: 1 });

        const address2 = "address-2";
        store.commit(ADD_ADDRESS_WEIGHT, { address: address2, weight: -2 });

        const address3 = "address-3";
        store.commit(ADD_ADDRESS_WEIGHT, { address: address3, weight: 3 });

        const address4 = "address-4";
        store.commit(ADD_ADDRESS_WEIGHT, { address: address4, weight: -4 });

        const { excludedAddresses } = store.getters[ADDRESS_AUTOCOMPLETE];
        expect(excludedAddresses[0]).toEqual(address2);
        expect(excludedAddresses[1]).toEqual(address4);
    });
});
