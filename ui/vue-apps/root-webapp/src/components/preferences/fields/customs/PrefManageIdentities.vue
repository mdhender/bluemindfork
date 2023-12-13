<template>
    <div class="pref-manage-identities">
        <bm-table
            :items="identities"
            :fields="fields"
            :per-page="perPage"
            :current-page="currentPage"
            sort-by="isDefault"
            sort-desc
        >
            <template #cell(isDefault)="row">
                <div :title="$t('preferences.mail.identities.default')">
                    <bm-icon v-if="row.value" icon="star-fill" size="lg" />
                </div>
            </template>
            <template #cell(displayname)="row">
                <contact :contact="getContact(row.item)" show-address transparent bold-dn />
            </template>
            <template #cell(name)="row">{{ row.value }}</template>
            <template #cell(action)="row">
                <bm-button variant="text" size="lg" @click="openModal(row.item)">
                    {{ $t("common.manage") }}
                </bm-button>
            </template>
        </bm-table>
        <bm-pagination v-model="currentPage" :total-rows="totalRows" :per-page="perPage" class="d-inline-flex" />
        <bm-button variant="outline" size="lg" icon="plus" @click="openModal()">
            {{ $t("preferences.mail.identities.create") }}
        </bm-button>
        <manage-identity-modal
            ref="manage-identity"
            :possible-identities="possibleIdentities"
            :possible-identities-status="possibleIdentitiesStatus"
        />
    </div>
</template>

<script>
import { mapState } from "vuex";
import { inject } from "@bluemind/inject";
import { BmButton, BmIcon, BmPagination, BmTable } from "@bluemind/ui-components";
import { Contact } from "@bluemind/business-components";

import ManageIdentityModal from "./ManageIdentityModal";
import { BaseField } from "@bluemind/preferences";

export default {
    name: "PrefManageIdentities",
    components: {
        BmButton,
        BmIcon,
        BmPagination,
        BmTable,
        Contact,
        ManageIdentityModal
    },
    mixins: [BaseField],
    data() {
        return {
            possibleIdentities: [],
            possibleIdentitiesStatus: "NOT-LOADED",

            currentPage: 1,
            perPage: 5,
            fields: [
                {
                    key: "isDefault",
                    headerTitle: this.$t("preferences.mail.identities.default"),
                    label: "",
                    class: "is-default-cell"
                },
                {
                    key: "displayname",
                    label: this.$t("common.identity"),
                    class: "identity-cell text-truncate"
                },
                {
                    key: "name",
                    label: this.$t("common.label"),
                    class: "label-cell text-nowrap text-truncate"
                },
                {
                    key: "action",
                    headerTitle: this.$t("common.action"),
                    label: "",
                    class: "action-cell"
                }
            ]
        };
    },
    computed: {
        ...mapState("root-app", ["identities"]),
        totalRows() {
            return this.identities.length;
        }
    },
    methods: {
        getContact(identity) {
            return { address: identity.email, dn: identity.displayname };
        },
        async openModal(identity) {
            this.$refs["manage-identity"].open(identity);
            if (this.possibleIdentitiesStatus !== "LOADED") {
                this.possibleIdentitiesStatus = "LOADING";
                try {
                    this.possibleIdentities = await inject("UserMailIdentitiesPersistence").getAvailableIdentities();
                    this.possibleIdentitiesStatus = "LOADED";
                } catch {
                    this.possibleIdentitiesStatus = "ERROR";
                }
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-manage-identities {
    .b-table {
        max-width: base-px-to-rem(900);
        table-layout: fixed;
    }
    .is-default-cell {
        width: base-px-to-rem(40);
        padding-bottom: base-px-to-rem(4) !important;
        .icon-star-fill {
            color: $secondary-fg;
        }
    }
    .identity-cell {
        width: 100%;
        .contact {
            max-width: 100%;
        }
    }
    td.identity-cell {
        padding-top: base-px-to-rem(4) !important;
    }
    .label-cell {
        width: 50%;
    }
    .action-cell {
        width: base-px-to-rem(75);
    }
}
</style>
