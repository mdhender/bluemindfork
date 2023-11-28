<template>
    <div class="pref-ext-account-creation">
        <template v-if="externalSystems.length > 0">
            <pref-ext-account-modal
                id="ext-account-editing-modal"
                ref="ext-account-editing-modal"
                :external-account="editingExternalAccount"
                @updateExternalAccount="addExternalAccount"
            />
            <p class="regular-medium mb-5">{{ $t("preferences.account.external_accounts.creation.desc") }}</p>
            <p class="bold mb-5">{{ $t("preferences.account.external_accounts.creation.add") }}</p>
            <div class="buttons">
                <bm-button
                    v-for="(externalSystem, index) in externalSystems"
                    :key="index"
                    :disabled="externalAccounts.some(ea => ea.identifier === externalSystem.identifier)"
                    variant="outline"
                    @click="createExternalAccount(externalSystem)"
                >
                    <template #icon>
                        <img :src="externalSystem.logo.src" alt="" :title="externalSystem.description" />
                    </template>
                    {{ externalSystem.properties["name"] || externalSystem.identifier }}
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
import { BmButton } from "@bluemind/ui-components";
import { BaseField } from "@bluemind/preferences";
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
    const externalSystems =
        (await service.getExternalSystemsByAuthKind(["NONE", "SIMPLE_CREDENTIALS", "API_KEY"])) || [];
    const result = await Promise.all(
        externalSystems.map(async externalSystem => {
            const logoImageData = await service.getLogo(externalSystem.identifier);
            return {
                ...externalSystem,
                logo: {
                    src: `data:image/png;base64,${logoImageData}`,
                    alt: externalSystem.properties["name"] || externalSystem.identifier,
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
                      auth: externalSystem.authKind,
                      name: externalSystem.properties["name"] || externalSystem.identifier
                  }
                : undefined;
        })
        .filter(Boolean);
}
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-ext-account-creation {
    .buttons {
        display: flex;
        flex-wrap: wrap;
        gap: $sp-5;
        @include from-lg {
            gap: $sp-6;
        }

        > .bm-button {
            width: base-px-to-rem(160);
            flex: none;

            flex-direction: column;
            gap: $sp-5 + $sp-3 !important;
            padding: calc(#{$sp-5 + $sp-3} - 1px) !important;
            padding-top: calc(#{$sp-6} - 1px) !important;

            .slot-wrapper {
                width: 100%;
            }

            img {
                width: base-px-to-rem(70);
                height: base-px-to-rem(44);
            }

            &:disabled img {
                opacity: 0.5;
            }
        }
    }
}
</style>
