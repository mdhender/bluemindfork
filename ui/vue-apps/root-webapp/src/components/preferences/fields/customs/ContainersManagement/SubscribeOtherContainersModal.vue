<template>
    <bm-modal
        v-model="show"
        centered
        lazy
        modal-class="add-containers-modal"
        :title="modalTitle"
        :cancel-title="$t('common.cancel')"
        :ok-title="okTitle"
        :ok-only="allReadableContainers.length === 0"
        :ok-disabled="allReadableContainers.length > 0 && selected.length === 0"
        @ok="subscribe"
    >
        <bm-spinner v-if="loadingStatus === 'LOADING'" :size="2" class="d-flex justify-content-center" />
        <template v-else>
            <div class="selected-containers">
                <div v-for="container in selected" :key="container.uid" class="d-inline-block">
                    <slot name="selected" :container="container" :closeFn="removeFromSelected" />
                </div>
            </div>
            <template v-if="allReadableContainers.length > 0">
                <bm-form-input
                    v-model="pattern"
                    :placeholder="searchPlaceholder"
                    icon="search"
                    :class="{ 'mt-3': selected.length !== 0 }"
                    class="mb-3"
                    resettable
                    left-icon
                    :aria-label="searchPlaceholder"
                    autocomplete="off"
                    @reset="pattern = ''"
                />
                <bm-table
                    v-if="suggested.length > 0"
                    :items="suggested"
                    :fields="fields"
                    :per-page="perPage"
                    :current-page="currentPage"
                    sort-by="name"
                    @row-clicked="toggleSelected"
                >
                    <template #cell(selected)="row">
                        <bm-form-checkbox :checked="isSelected(row.item)" @change="toggleSelected(row.item)" />
                    </template>
                    <template #cell(name)="row">
                        <slot name="item" :container="row.item" />
                    </template>
                    <template #cell(ownerDisplayname)="row">
                        <span class="font-italic text-neutral text-truncate">
                            {{ $t("common.shared_by", { name: row.value }) }}
                        </span>
                    </template>
                </bm-table>
                <div v-else>{{ $t("common.search.no_result") }}</div>
                <bm-pagination v-model="currentPage" :total-rows="totalRows" :per-page="perPage" />
            </template>
            <div v-else>{{ $t("preferences.add_containers." + containerType + ".nothing_to_add") }}</div>
        </template>
    </bm-modal>
</template>

<script>
import { inject } from "@bluemind/inject";
import { BmFormCheckbox, BmFormInput, BmModal, BmPagination, BmSpinner, BmTable } from "@bluemind/styleguide";
import { mapActions } from "vuex";
import { SUCCESS } from "@bluemind/alert.store";
import { ContainerType } from "./container";
import { SAVE_ALERT } from "../../../Alerts/defaultAlerts";

export default {
    name: "SubscribeOtherContainersModal",
    components: { BmFormCheckbox, BmFormInput, BmModal, BmPagination, BmSpinner, BmTable },
    props: {
        containerType: {
            type: String,
            required: true
        },
        excludedContainers: {
            type: Array,
            required: true
        },
        fieldId: {
            type: String,
            required: true
        }
    },
    data() {
        return {
            show: false,
            loadingStatus: "IDLE",
            selected: [],
            pattern: "",
            allReadableContainers: [],
            currentPage: 1,
            perPage: 10,
            fields: [
                {
                    key: "selected",
                    sortable: true,
                    headerTitle: this.$t("common.selection"),
                    label: "",
                    class: "selected-cell"
                },
                {
                    key: "name",
                    sortable: true,
                    headerTitle: this.$t("common.label"),
                    label: "",
                    class: "name-cell text-truncate"
                },
                {
                    key: "ownerDisplayname",
                    headerTitle: this.$t("common.shared_by"),
                    label: "",
                    class: "shared-by-cell text-truncate"
                }
            ]
        };
    },
    computed: {
        searchPlaceholder() {
            return this.$t("preferences.add_containers.search", {
                type: this.$t("common.container_type_with_indefinite_article." + this.containerType)
            });
        },
        modalTitle() {
            return this.$t("preferences.add_containers.subscribe", {
                type: this.$tc("common.container_type." + this.containerType, 2)
            });
        },
        okTitle() {
            if (!this.hasReadableContainers) {
                return this.$t("common.got_it");
            } else {
                return this.$tc("preferences.add_containers.add_n_containers", this.selected.length, {
                    count: this.selected.length,
                    type: this.$tc("common.container_type." + this.containerType, this.selected.length)
                });
            }
        },
        hasReadableContainers() {
            return this.allReadableContainers.length > 0;
        },
        suggested() {
            const realPattern = this.pattern.toLowerCase();
            return this.allReadableContainers.filter(
                mailbox =>
                    mailbox.name.toLowerCase().includes(realPattern) ||
                    mailbox.ownerDisplayname.toLowerCase().includes(realPattern)
            );
        },
        totalRows() {
            return this.suggested.length;
        }
    },
    methods: {
        ...mapActions("preferences", ["SUBSCRIBE_TO_CONTAINERS"]),
        ...mapActions("alert", { SUCCESS }),
        async open() {
            this.loadingStatus = "LOADING";
            this.show = true;
            this.selected = [];
            this.currentPage = 1;
            this.pattern = "";
            this.allReadableContainers = await this.loadContainers();
            this.loadingStatus = "LOADED";
        },
        async loadContainers() {
            const containers = await inject("ContainersPersistence").all({ type: this.containerType });
            return containers.filter(
                mailbox =>
                    mailbox.owner !== inject("UserSession").userId &&
                    this.excludedContainers.findIndex(excluded => excluded.uid === mailbox.uid) === -1
            );
        },

        toggleSelected(container) {
            const index = this.selected.findIndex(selectedContainer => selectedContainer.uid === container.uid);
            if (index === -1) {
                this.selected.push({ ...container });
            } else {
                this.selected.splice(index, 1);
            }
        },
        isSelected(container) {
            return this.selected.findIndex(selectedContainer => selectedContainer.uid === container.uid) !== -1;
        },
        removeFromSelected(container) {
            const index = this.selected.findIndex(selectedContainer => selectedContainer.uid === container.uid);
            if (index !== -1) {
                this.selected.splice(index, 1);
            }
        },

        async subscribe() {
            if (this.hasReadableContainers) {
                const containers = this.selected.map(container => ({ ...container, offlineSync: true }));
                await this.SUBSCRIBE_TO_CONTAINERS(containers);
                if (this.containerType === ContainerType.MAILBOX && this.$route.path.startsWith("/mail/")) {
                    this.$store.commit("preferences/fields/NEED_RELOAD", { id: this.fieldId });
                }
                this.$emit("subscribe", containers);
                this.SUCCESS(SAVE_ALERT);
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/mixins/_responsiveness";
@import "~@bluemind/styleguide/css/_variables";

.add-containers-modal {
    .selected-containers {
        display: flex;
        flex-wrap: wrap;
        gap: $sp-3 $sp-4;
        padding-bottom: $sp-4;
    }

    .b-table {
        max-width: base-px-to-rem(500);
        table-layout: fixed;
    }
    .selected-cell {
        width: base-px-to-rem(40);
    }
    .name-cell {
        width: 100%;
        .bm-contact {
            margin-top: base-px-to-rem(4);
        }
    }
    .shared-by-cell {
        display: none;
        @include from-lg {
            display: table-cell;
            width: 100%;
        }
    }

    .b-table tr {
        cursor: pointer;
    }
}
</style>
