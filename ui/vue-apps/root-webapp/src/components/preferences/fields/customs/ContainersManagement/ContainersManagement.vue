<template>
    <div class="containers-management">
        <template v-if="containers.length > 0">
            <bm-form-input
                v-if="canFilter"
                v-model="pattern"
                class="mt-1 mb-3"
                :placeholder="$t('common.filter')"
                icon="filter"
                resettable
                left-icon
                :aria-label="$t('common.filter')"
                autocomplete="off"
                @reset="pattern = ''"
            />
            <bm-table
                v-if="filtered.length > 0"
                :items="filtered"
                :fields="fields"
                :per-page="perPage"
                :current-page="currentPage"
                :sort-by="sortBy"
                :sort-desc="sortDesc"
            >
                <template #cell(defaultContainer)="row">
                    <slot name="default-container-column" :isDefault="row.value" />
                </template>
                <template #cell(name)="row"><slot name="item" :container="row.item" /></template>
                <template #cell(ownerDisplayname)="row">
                    <span class="font-italic text-secondary">{{ $t("common.shared_by", { name: row.value }) }}</span>
                </template>
                <template #cell(share)="row">
                    <bm-button v-if="isManaged(row.item)" variant="inline" @click="openShareModal(row.item)">
                        <bm-icon icon="share" size="lg" />
                    </bm-button>
                </template>
                <template #cell(offlineSync)="row">
                    <bm-form-checkbox :checked="row.value" switch @change="toggleOfflineSync(row.item)" />
                </template>
                <template #cell(action)="row">
                    <slot
                        name="action"
                        :container="row.item"
                        :openShareModal="openShareModal"
                        :isManaged="isManaged"
                        :toggleSubscription="toggleSubscription"
                    >
                        <bm-button variant="outline-secondary" @click="toggleSubscription(row.item)">
                            <template v-if="isSubscribed(row.item)">{{ $t("common.unsubscribe") }}</template>
                            <template v-else>{{ $t("common.subscribe") }}</template>
                        </bm-button>
                    </slot>
                </template>
            </bm-table>
            <div v-else>{{ $t("common.search.no_result") }}</div>
            <bm-pagination v-model="currentPage" :total-rows="totalRows" :per-page="perPage" class="d-inline-flex" />
        </template>
        <div v-else>{{ $t("preferences.display_containers." + containerType + ".empty_list") }}</div>
        <bm-button variant="outline-secondary" class="float-right" @click="actionBtnListener">
            <slot name="action-btn-content">{{ defaultBottomRightBtnLabel }}</slot>
        </bm-button>
        <manage-container-shares-modal ref="manage-shares" />
        <add-containers-modal
            v-if="defaultActionModal"
            ref="add-containers"
            :container-type="containerType"
            :excluded-containers="containers"
            @add-containers="containers => $emit('add', containers)"
        >
            <template v-slot:selected="{ container, closeFn }">
                <slot name="badge-item" :container="container" :closeFn="closeFn" />
            </template>
            <template v-slot:item="{ container }">
                <slot name="item" :container="container" />
            </template>
        </add-containers-modal>
        <slot name="additionnal-modals" />
    </div>
</template>

<script>
import { containerToSubscription } from "./container";
import AddContainersModal from "./AddContainersModal";
import ManageContainerSharesModal from "./ManageContainerSharesModal/ManageContainerSharesModal";
import { Verb } from "@bluemind/core.container.api";
import { inject } from "@bluemind/inject";
import { BmButton, BmFormCheckbox, BmFormInput, BmIcon, BmPagination, BmTable } from "@bluemind/styleguide";
import { mapActions, mapState } from "vuex";

export default {
    name: "ContainersManagement",
    components: {
        AddContainersModal,
        BmButton,
        BmFormCheckbox,
        BmFormInput,
        BmIcon,
        BmPagination,
        BmTable,
        ManageContainerSharesModal
    },
    props: {
        containers: {
            type: Array,
            required: true
        },
        containerType: {
            type: String,
            required: true
        },
        hasShareColumn: {
            type: Boolean,
            default: true
        },
        defaultContainerField: {
            type: [Boolean, Object],
            default: false
        },
        canFilter: {
            type: Boolean,
            default: true
        },
        sortBy: {
            type: String,
            default: "name"
        },
        sortDesc: {
            type: Boolean,
            default: false
        },
        defaultActionModal: {
            type: Boolean,
            default: true
        }
    },
    data() {
        return { currentPage: 1, perPage: 5, pattern: "" };
    },
    computed: {
        ...mapState("preferences", ["subscriptions"]),
        defaultBottomRightBtnLabel() {
            return this.$t("preferences.add_containers.subscribe", {
                type: this.$tc("common.container_type." + this.containerType, 2)
            });
        },
        fields() {
            const fields = [
                { key: "name", sortable: true, label: this.$t("common.label") },
                { key: "ownerDisplayname", headerTitle: this.$t("common.shared_by"), label: "" },
                { key: "offlineSync", label: this.$t("common.synchronization"), sortable: true },
                { key: "action", headerTitle: this.$t("common.action"), label: "", class: "text-right" }
            ];
            if (this.hasShareColumn) {
                fields.splice(2, 0, { key: "share", label: this.$t("common.sharing") });
            }
            if (this.defaultContainerField) {
                fields.splice(0, 0, this.defaultContainerField);
            }
            return fields;
        },
        filtered() {
            const realPattern = this.pattern.toLowerCase();
            return this.containers.filter(
                container =>
                    container.name.toLowerCase().includes(realPattern) ||
                    container.ownerDisplayname.toLowerCase().includes(realPattern)
            );
        },
        totalRows() {
            return this.filtered.length;
        }
    },
    methods: {
        ...mapActions("preferences", ["SET_SUBSCRIPTIONS", "REMOVE_SUBSCRIPTIONS"]),
        isManaged(container) {
            return container.verbs.some(verb => verb === Verb.All || verb === Verb.Manage);
        },
        isSubscribed(container) {
            return this.subscriptions.findIndex(sub => sub.value.containerUid === container.uid) !== -1;
        },
        async toggleOfflineSync(container) {
            const updatedContainer = { ...container, offlineSync: !container.offlineSync };
            const subscription = containerToSubscription(inject("UserSession"), updatedContainer);
            await this.SET_SUBSCRIPTIONS([subscription]);
            this.$emit("update", updatedContainer);
        },
        async toggleSubscription(container) {
            if (this.isSubscribed(container)) {
                await this.REMOVE_SUBSCRIPTIONS([container.uid]);
                if (!this.isManaged(container)) {
                    this.$emit("remove", container.uid);
                } else {
                    const updatedContainer = { ...container, offlineSync: false };
                    this.$emit("update", updatedContainer);
                }
            } else {
                const updatedContainer = { ...container, offlineSync: true };
                const subscription = containerToSubscription(inject("UserSession"), updatedContainer);
                await this.SET_SUBSCRIPTIONS([subscription]);
                this.$emit("update", updatedContainer);
            }
        },
        openShareModal(container) {
            this.$refs["manage-shares"].open(container);
        },
        actionBtnListener() {
            if (this.defaultActionModal) {
                this.$refs["add-containers"].open();
            } else {
                this.$emit("action-btn-clicked");
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.containers-management .b-table .fa-star-fill {
    color: $primary;
}
</style>
