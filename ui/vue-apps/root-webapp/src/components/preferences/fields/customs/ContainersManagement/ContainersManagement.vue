<template>
    <div class="containers-management">
        <template v-if="containers.length > 0">
            <bm-form-input
                v-if="!manageMine"
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
            <containers-management-table
                v-if="filtered.length > 0"
                :container-type="containerType"
                :filtered="filtered"
                :manage-mine="manageMine"
                :has-share-column="hasShareColumn"
                :per-page="perPage"
                :current-page="currentPage"
                @open-import-modal="openImportModal"
                @open-share-modal="openShareModal"
                @offline-sync-changed="container => $emit('offline-sync-changed', container)"
                @remove="container => $emit('remove', container)"
                @reset-data="container => $emit('reset-data', container)"
                @update="update"
            >
                <template v-slot:item="{ container }"><slot name="item" :container="container" /></template>
            </containers-management-table>
            <div v-else>{{ $t("common.search.no_result") }}</div>
            <bm-pagination v-model="currentPage" :total-rows="totalRows" :per-page="perPage" class="d-inline-flex" />
        </template>
        <div v-else>{{ $t("preferences.display_containers." + containerType + ".empty_list") }}</div>
        <bm-button variant="outline-secondary" class="float-right" @click="openBottomActionModal">
            <template v-if="manageMine"><bm-icon icon="plus" /> {{ createContainerLabel }} </template>
            <template v-else>{{ subscribeToContainerLabel }}</template>
        </bm-button>
        <create-or-update-container-modal
            ref="create-or-update-container"
            :containers="containers"
            @create="container => $emit('create', container)"
            @update="container => $emit('update', container)"
        />
        <import-modal ref="import" />
        <manage-container-shares-modal ref="manage-shares" />
        <subscribe-other-containers-modal
            ref="add-containers"
            :container-type="containerType"
            :excluded-containers="containers"
            @subscribe="containers => $emit('subscribe', containers)"
        >
            <template v-slot:selected="{ container, closeFn }">
                <slot name="badge-item" :container="container" :closeFn="closeFn" />
            </template>
            <template v-slot:item="{ container }">
                <slot name="item" :container="container" />
            </template>
        </subscribe-other-containers-modal>
    </div>
</template>

<script>
import { create, isManaged } from "./container";
import ContainersManagementTable from "./ContainersManagementTable";
import CreateOrUpdateContainerModal from "./CreateOrUpdateContainerModal";
import ImportModal from "./ImportModal";
import ManageContainerSharesModal from "./ManageContainerSharesModal/ManageContainerSharesModal";
import SubscribeOtherContainersModal from "./SubscribeOtherContainersModal";
import { BmButton, BmFormInput, BmIcon, BmPagination } from "@bluemind/styleguide";

export default {
    name: "ContainersManagement",
    components: {
        BmButton,
        BmFormInput,
        BmIcon,
        BmPagination,
        ContainersManagementTable,
        CreateOrUpdateContainerModal,
        ImportModal,
        ManageContainerSharesModal,
        SubscribeOtherContainersModal
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
        manageMine: {
            type: Boolean,
            default: false
        },
        hasShareColumn: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return { currentPage: 1, isManaged, perPage: 5, pattern: "" };
    },
    computed: {
        subscribeToContainerLabel() {
            return this.$t("preferences.add_containers.subscribe", {
                type: this.$tc("common.container_type." + this.containerType, 2)
            });
        },
        createContainerLabel() {
            return this.$t("preferences.create_container.button", {
                type: this.$t("common.container_type_with_indefinite_article." + this.containerType)
            });
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
        openBottomActionModal() {
            if (this.manageMine) {
                const container = create(this.containerType);
                this.$refs["create-or-update-container"].open(container);
            } else {
                this.$refs["add-containers"].open();
            }
        },
        openShareModal(container) {
            this.$refs["manage-shares"].open(container);
        },
        openImportModal(container) {
            this.$refs["import"].open(container);
        },
        update(container) {
            this.$refs["create-or-update-container"].open(container);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.containers-management {
    .bm-contextual-menu .dropdown-toggle {
        text-align: right;
    }
    .b-table .fa-star-fill {
        color: $primary;
    }
}
</style>
