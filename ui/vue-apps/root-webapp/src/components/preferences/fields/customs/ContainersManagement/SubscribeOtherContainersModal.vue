<template>
    <bm-spinner v-if="loadingStatus === 'LOADING'" class="subscribe-other-containers-spinner" />
    <bm-modal
        v-else-if="allReadableContainers.length === 0"
        v-model="show"
        size="sm"
        lazy
        :title="modalTitle"
        :ok-title="$t('common.got_it')"
        ok-only
    >
        {{ $t("preferences.add_containers." + containerType + ".nothing_to_add") }}
    </bm-modal>
    <bm-modal
        v-else
        v-model="show"
        variant="advanced"
        size="md"
        height="lg"
        lazy
        modal-class="add-containers-modal"
        :title="modalTitle"
        :cancel-title="$t('common.cancel')"
        :ok-title="okTitle"
        :ok-disabled="selected.length === 0"
        @ok="subscribe"
    >
        <div v-if="selected.length > 0" class="selected-containers">
            <div v-for="container in selected" :key="container.uid" class="d-inline-block">
                <slot name="selected" :container="container" :close-fn="removeFromSelected" />
            </div>
        </div>
        <bm-form-input
            v-model="pattern"
            :placeholder="searchPlaceholder"
            variant="underline"
            icon="magnifier"
            resettable
            left-icon
            :aria-label="searchPlaceholder"
            autocomplete="off"
            @reset="pattern = ''"
        />
        <div class="scrollable scroller-y-stable">
            <bm-table
                v-if="suggested.length > 0"
                :items="suggested"
                :fields="fields"
                :per-page="perPage"
                :current-page="currentPage"
                :fill="false"
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
            <div v-else class="no-search-result">
                {{ $t("common.search.no_result") }}
                <bm-illustration size="md" value="spider" over-background />
            </div>
            <bm-pagination v-model="currentPage" :total-rows="totalRows" :per-page="perPage" />
        </div>
    </bm-modal>
</template>

<script>
import { inject } from "@bluemind/inject";
import {
    BmFormCheckbox,
    BmFormInput,
    BmIllustration,
    BmModal,
    BmPagination,
    BmSpinner,
    BmTable
} from "@bluemind/ui-components";
import { mapActions } from "vuex";
import { SUCCESS } from "@bluemind/alert.store";
import { isContainerTypeUsedByApp } from "./container";
import { SAVE_ALERT } from "../../../Alerts/defaultAlerts";
import { matchPattern } from "@bluemind/string";
import { Verb } from "@bluemind/core.container.api";

export default {
    name: "SubscribeOtherContainersModal",
    components: { BmFormCheckbox, BmFormInput, BmIllustration, BmModal, BmPagination, BmSpinner, BmTable },
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
            perPage: 20,
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
            return this.$tc("preferences.add_containers.add_n_containers", this.selected.length, {
                count: this.selected.length,
                type: this.$tc("common.container_type." + this.containerType, this.selected.length)
            });
        },
        hasReadableContainers() {
            return this.allReadableContainers.length > 0;
        },
        suggested() {
            return this.allReadableContainers.filter(mailbox =>
                matchPattern(this.pattern, [mailbox.name, mailbox.ownerDisplayname])
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
            const containers = await inject("ContainersPersistence").all({
                type: this.containerType,
                verb: [Verb.Read, Verb.Write, Verb.Manage, Verb.All]
            });
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
                if (isContainerTypeUsedByApp(this.containerType, this.$route)) {
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
@use "sass:math";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.subscribe-other-containers-spinner {
    $spinner-half-size: math.div(map-get($icon-sizes, "5xl"), 2);
    position: fixed;
    top: calc(50vh - #{$spinner-half-size});
    left: calc(50vw - #{$spinner-half-size});
}

.add-containers-modal .modal-body {
    padding: 0 !important;
    display: flex;
    flex-direction: column;

    .selected-containers {
        flex: none;
        display: flex;
        flex-wrap: wrap;
        gap: ($sp-3 + $sp-2) $sp-5;
        padding: $sp-5;
        border-bottom: 1px solid $neutral-fg-lo2;
    }

    .scrollable {
        flex: 1;
        background-color: $backdrop;

        .b-table {
            background-color: $surface;
            table-layout: fixed;
            margin: 0 !important;

            thead {
                display: none;
            }
            tr {
                cursor: pointer;
                border-bottom: 1px solid $neutral-fg-lo3 !important;
            }

            .selected-cell {
                width: base-px-to-rem(40);
            }
            .name-cell {
                width: 100%;
                .contact {
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
        }

        .bm-pagination {
            background-color: $surface;
            margin: 0 !important;
            padding: $sp-5;
        }

        .no-search-result {
            height: 100%;
            display: flex;
            flex-direction: column;
            align-items: center;
            padding-top: $sp-7;
            gap: $sp-6;

            .bm-illustration {
                flex: none;
            }
        }
    }
}
</style>
