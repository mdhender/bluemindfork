<template>
    <div class="pref-ext-account-creation">
        <template v-if="externalSystems.length > 0">
            <pref-ext-account-modal
                id="ext-account-editing-modal"
                ref="ext-account-editing-modal"
                :external-account="editingExternalAccount"
                @updateExternalAccount="addExternalAccount"
            />
            <p>{{ $t("preferences.account.external_accounts.creation.desc") }}</p>
            <p>{{ $t("preferences.account.external_accounts.creation.add") }}</p>
            <div class="d-flex">
                <bm-button
                    v-for="(externalSystem, index) in externalSystems"
                    :key="index"
                    :disabled="externalAccounts.some(ea => ea.identifier === externalSystem.identifier)"
                    variant="outline"
                    class="pref-ext-account-creation-button px-3 p-3 m-1 d-flex flex-column justify-content-between"
                    @click="createExternalAccount(externalSystem)"
                >
                    <div class="flex-grow-1 d-flex align-items-center">
                        <img
                            :src="externalSystem.logo.src"
                            :alt="externalSystem.identifier"
                            :title="externalSystem.description"
                        />
                    </div>
                    <h2 class="mt-3">
                        {{ externalSystem.identifier }}
                    </h2>
                </bm-button>
            </div>
        </template>
        <em v-else>
            {{ $t("preferences.account.external_accounts.creation.none") }}
        </em>
    </div>
</template>

<script>
import { inject } from "@bluemind/inject";
import { BmButton } from "@bluemind/styleguide";
import BaseField from "../../../mixins/BaseField";
import PrefExtAccountModal from "./PrefExtAccountModal";
import { mapMutations } from "vuex";

export default {
    name: "PrefExtAccountCreation",
    components: { BmButton, PrefExtAccountModal },
    mixins: [BaseField],
    data() {
        return {
            externalSystems: [],
            editingExternalAccount: {},
            listFieldId: "my_account.external_accounts.list.field"
        };
    },
    computed: {
        externalAccounts() {
            return this.$store.state.preferences.fields[this.listFieldId].current?.value || [];
        }
    },
    async created() {
        this.externalSystems = await fetchExternalSystems();
        const externalAccounts = await fetchAndConsolidateExternalAccounts(this.externalSystems);
        this.SET_EXTERNAL_ACCOUNTS(externalAccounts);
    },
    methods: {
        ...mapMutations("preferences", ["SET_EXTERNAL_ACCOUNTS"]),
        addExternalAccount(externalAccount) {
            externalAccount.isNew = false;
            const current = this.$store.state.preferences.fields[this.listFieldId].current;
            this.PUSH_STATE({
                id: this.listFieldId,
                options: { saved: false, error: false, autosave: true },
                value: [...current.value, externalAccount]
            });
            this.$store.dispatch("preferences/AUTOSAVE");
        },
        createExternalAccount(externalSystem) {
            this.editingExternalAccount = { ...externalSystem, isNew: true };
            this.$refs["ext-account-editing-modal"].show();
        }
    }
};

async function fetchExternalSystems() {
    const service = inject("ExternalSystemPersistence");
    const externalSystems = (await service.getExternalSystems()) || [];
    const result = await Promise.all(
        externalSystems.map(async externalSystem => {
            const logoImageData = await service.getLogo(externalSystem.identifier);
            return {
                ...externalSystem,
                logo: {
                    src: `data:image/png;base64,${logoImageData}`,
                    alt: externalSystem.identifier,
                    title: externalSystem.description
                }
            };
        })
    );
    return result;
}

async function fetchAndConsolidateExternalAccounts(externalSystems) {
    const rawExternalAccounts = await inject("UserExternalAccountPersistence").getAll();
    return rawExternalAccounts
        .map(rawExternalAccount => {
            const identifier = rawExternalAccount.externalSystemId;
            const externalSystem = externalSystems.find(es => es.identifier === identifier);
            return externalSystem
                ? {
                      logo: externalSystem.logo,
                      identifier,
                      login: rawExternalAccount.login,
                      auth: externalSystem.authKind
                  }
                : undefined;
        })
        .filter(Boolean);
}
</script>

<style lang="scss" scoped>
@import "@bluemind/styleguide/css/_variables.scss";

.pref-ext-account-creation {
    .pref-ext-account-creation-button {
        background-color: $blue-100;
        &:focus {
            outline: 1px $neutral-fg dashed;
        }
    }

    h2 {
        color: $neutral-fg-hi1;
    }
}
</style>
