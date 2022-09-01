<template>
    <div class="containers-management">
        <a v-if="readMore" target="_blank" :href="readMore.href" class="mb-3">{{ readMore.text }}</a>
        <template v-if="containers.length > 0">
            <bm-form-input
                v-if="!manageMine && !collapsed"
                v-model="pattern"
                class="pref-filter mt-2 mb-3"
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
                :share-column="showShareColumn"
                :per-page="perPage"
                :current-page="currentPage"
                :field-id="fieldId"
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
        <div>
            <bm-button variant="outline" size="lg" icon="plus" @click="openBottomActionModal">
                {{ manageMine ? createContainerLabel : subscribeToContainerLabel }}
            </bm-button>
        </div>
        <create-or-update-container-modal
            ref="create-or-update-container"
            :containers="containers"
            :create-fn="createContainerFn"
            @update="container => $emit('update', container)"
        />
        <import-modal ref="import" />
        <manage-container-shares-modal ref="manage-shares" />
        <subscribe-other-containers-modal
            ref="add-containers"
            :container-type="containerType"
            :excluded-containers="containers"
            :field-id="fieldId"
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
import { BmButton, BmFormInput, BmPagination } from "@bluemind/styleguide";

export default {
    name: "ContainersManagement",
    components: {
        BmButton,
        BmFormInput,
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
        createContainerFn: {
            type: Function,
            default: () => {}
        },
        manageMine: {
            type: Boolean,
            default: false
        },
        shareColumn: {
            type: Boolean,
            default: false
        },
        collapsed: {
            type: Boolean,
            required: true
        },
        fieldId: {
            type: String,
            required: true
        },
        readMore: {
            type: Object,
            default: undefined
        }
    },
    data() {
        return { currentPage: 1, isManaged, pattern: "" };
    },
    computed: {
        perPage() {
            return this.collapsed ? 2 : 5;
        },
        subscribeToContainerLabel() {
            return this.$t("preferences.add_containers.subscribe", {
                type: this.$tc("common.container_type." + this.containerType, 2)
            });
        },
        createContainerLabel() {
            return this.$t("preferences.create_container." + this.containerType + ".button");
        },
        filtered() {
            const realPattern = this.pattern.toLowerCase();
            return this.containers.filter(
                container =>
                    container.name.toLowerCase().includes(realPattern) ||
                    container.ownerDisplayname.toLowerCase().includes(realPattern)
            );
        },
        showShareColumn() {
            return this.shareColumn && this.containers.some(this.isManaged);
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
    .b-table .fa-star-fill {
        color: $secondary-fg;
    }
}
</style>
