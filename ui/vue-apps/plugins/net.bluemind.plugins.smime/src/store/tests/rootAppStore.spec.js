import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import storeOptions from "../root-app/index";
import { PKIStatus } from "../../lib/constants";
import { CHECK_IF_ASSOCIATED, DISSOCIATE_CRYPTO_FILES, SET_SW_AVAILABLE, SMIME_AVAILABLE } from "../root-app/types";

Vue.use(Vuex);

describe("smime store", () => {
    let store;
    beforeEach(() => {
        store = new Vuex.Store(cloneDeep(storeOptions));
        store.commit(SET_SW_AVAILABLE, true);
    });

    describe("associate & dissociate cert and private key", () => {
        test("[CHECK_IF_ASSOCIATED] smime is available if cert and key are available in service-worker", async () => {
            expect(store.state.isServiceWorkerAvailable).toBe(true); // mocked
            expect(store.getters[SMIME_AVAILABLE]).toBe(false);
            global.fetch = () =>
                Promise.resolve({
                    json: () => Promise.resolve(PKIStatus.OK)
                });
            await store.dispatch(CHECK_IF_ASSOCIATED);
            expect(store.getters[SMIME_AVAILABLE]).toBe(true);
            expect(store.state.swError).toBe(false);

            global.fetch = () =>
                Promise.resolve({
                    json: () => Promise.resolve(PKIStatus.CERTIFICATE_OK)
                });
            await store.dispatch(CHECK_IF_ASSOCIATED);
            expect(store.getters[SMIME_AVAILABLE]).toBe(false);
            expect(store.state.swError).toBe(false);

            global.fetch = () => Promise.reject();
            await store.dispatch(CHECK_IF_ASSOCIATED);
            expect(store.state.swError).toBe(true);
        });

        test("DISSOCIATE_CRYPTO_FILES", async () => {
            store.state.hasPrivateKey = true;
            store.state.hasPublicCert = true;
            expect(store.getters[SMIME_AVAILABLE]).toBe(true);

            global.fetch = () => Promise.resolve();
            await store.dispatch(DISSOCIATE_CRYPTO_FILES);
            expect(store.getters[SMIME_AVAILABLE]).toBe(false);
            expect(store.state.swError).toBe(false);

            global.fetch = () => Promise.reject();
            await store.dispatch(DISSOCIATE_CRYPTO_FILES);
            expect(store.state.swError).toBe(true);
        });
    });
});
