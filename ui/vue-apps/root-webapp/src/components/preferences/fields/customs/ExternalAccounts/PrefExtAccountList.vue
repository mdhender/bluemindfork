<template>
    <div class="pref-ext-account-list">
        <pref-ext-account-modal
            id="ext-account-editing-modal"
            ref="ext-account-editing-modal"
            :external-account="editingExternalAccount"
            @updateExternalAccount="update"
        />
        <template v-if="value && value.length > 0">
            <bm-table :items="value" :fields="fields" :per-page="5" :current-page="currentPage" sort-by="label">
                <template #cell(logo)="cell">
                    <img :src="cell.value.src" :alt="cell.value.identifier" :title="cell.value.description" />
                </template>
                <template #cell(identifier)="cell">
                    <span class="text-neutral">{{ cell.value }}</span>
                </template>
                <template #cell(login)="cell">
                    <strong>{{ cell.value }}</strong>
                </template>
                <template #cell(actions)="cell">
                    <div class="actions-cell">
                        <bm-icon-button variant="compact" icon="pencil" @click="editExternalAccount(cell.item)" />
                        <bm-icon-button variant="compact" icon="trash" @click="confirmRemove(cell.item)" />
                    </div>
                </template>
            </bm-table>
            <bm-pagination v-model="currentPage" :total-rows="value.length" :per-page="5" />
        </template>
        <em v-else>{{ $t("preferences.account.external_accounts.list.none") }}</em>
    </div>
</template>

<script>
import isEqual from "lodash.isequal";
import { inject } from "@bluemind/inject";
import { BmIconButton, BmPagination, BmTable } from "@bluemind/ui-components";
import CentralizedSaving from "../../../mixins/CentralizedSaving";
import PrefExtAccountModal from "./PrefExtAccountModal";
import { mapState } from "vuex";

export default {
    name: "PrefExtAccountList",
    components: { BmIconButton, BmPagination, BmTable, PrefExtAccountModal },
    mixins: [CentralizedSaving],
    data() {
        return {
            currentPage: 1,
            fields: [
                { key: "logo", label: "", class: "col-1" },
                { key: "identifier", label: "", class: "col-3 text-left align-middle" },
                { key: "login", label: "", class: "col-6 text-left align-middle" },
                { key: "actions", label: "", class: "col-2" }
            ],
            editingExternalAccount: {}
        };
    },
    computed: {
        ...mapState("preferences", ["externalAccounts"])
    },
    watch: {
        externalAccounts(value) {
            this.value = value;
        }
    },
    async created() {
        this.registerSaveAction(async ({ state: { current, saved } }) => {
            if (current && !current.options.saved) {
                try {
                    const added = current.value.filter(
                        cea => !saved.value.some(sea => sea.identifier === cea.identifier)
                    );
                    added.forEach(createExternalAccount);

                    const removed = saved.value.filter(
                        sea => !current.value.some(cea => cea.identifier === sea.identifier)
                    );
                    removed.forEach(removeExternalAccount);

                    const modified = current.value.filter(
                        cea =>
                            !isEqual(
                                saved.value.find(sea => sea.identifier === cea.identifier),
                                cea
                            )
                    );
                    modified.forEach(updateExternalAccount);

                    this.PUSH_STATE({ value: current.value, options: { saved: true } });
                } catch {
                    this.PUSH_STATE(saved);
                }
            }
        });
    },
    methods: {
        update(externalAccount) {
            const index = this.value.findIndex(ea => ea.identifier === externalAccount.identifier);
            index >= 0 ? this.value.splice(index, 1, externalAccount) : this.value.push(externalAccount);
        },
        remove(externalAccount) {
            const index = this.value.findIndex(ea => ea.identifier === externalAccount.identifier);
            this.value.splice(index, 1);
        },
        async confirmRemove(item) {
            const confirm = await this.$bvModal.msgBoxConfirm(
                this.$t("preferences.account.external_accounts.list.remove.modal.desc", {
                    name: item.identifier,
                    login: item.login
                }),
                {
                    title: this.$t("preferences.account.external_accounts.list.remove.modal.title"),
                    okTitle: this.$t("common.delete"),
                    cancelTitle: this.$t("common.cancel"),
                    okVariant: "fill-accent",
                    cancelVariant: "text",
                    centered: true,
                    hideHeaderClose: false,
                    autoFocusButton: "ok"
                }
            );

            if (confirm) {
                this.remove(item);
            }
        },
        editExternalAccount(externalAccount) {
            this.editingExternalAccount = { ...externalAccount };
            this.$refs["ext-account-editing-modal"].show();
        }
    }
};

function createExternalAccount(externalAccount) {
    const service = inject("UserExternalAccountPersistence");
    service.create(externalAccount.identifier, {
        login: externalAccount.login,
        credentials: externalAccount.credentials,
        additionalSettings: externalAccount.additionalSettings || {}
    });
}

function updateExternalAccount(externalAccount) {
    const service = inject("UserExternalAccountPersistence");
    service.update(externalAccount.identifier, {
        login: externalAccount.login,
        credentials: externalAccount.credentials,
        additionalSettings: externalAccount.additionalSettings || {}
    });
}

function removeExternalAccount(externalAccount) {
    const service = inject("UserExternalAccountPersistence");
    service.remove(externalAccount.identifier);
}
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.pref-ext-account-list {
    img {
        height: 2.25em;
    }

    .actions-cell {
        display: flex;
        justify-content: end;
        gap: $sp-5;
    }
}
</style>
