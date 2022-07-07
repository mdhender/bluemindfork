<template>
    <bm-table
        :items="filtered"
        :fields="fields"
        :per-page="perPage"
        :current-page="currentPage"
        :sort-by="sortBy"
        :sort-desc="sortDesc"
        class="containers-management-table"
    >
        <template #cell(defaultContainer)="row">
            <div v-if="isDefault(row.item.uid)" :title="defaultColumnTitle" class="text-center">
                <bm-icon icon="star-fill" size="lg" />
            </div>
        </template>
        <template #cell(name)="row"><slot name="item" :container="row.item" /></template>
        <template #cell(ownerDisplayname)="row">
            <span class="font-italic text-neutral">{{ $t("common.shared_by", { name: row.value }) }}</span>
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
            <manage-my-container-menu
                v-if="manageMine"
                :container="row.item"
                :is-sync-in-progress="!!syncInProgress[row.item.uid]"
                @update="$emit('update', row.item)"
                @manage-shares="openShareModal(row.item)"
                @import="$emit('open-import-modal', row.item)"
                @reset-data="resetData(row.item)"
                @remove="remove(row.item)"
                @synchronize="sync(row.item)"
            />
            <template v-else-if="containerType === ContainerType.CALENDAR">
                <bm-button v-if="isManaged(row.item)" variant="inline" @click="openShareModal(row.item)">
                    <bm-icon icon="share" size="lg" />
                </bm-button>
                <bm-button v-else variant="inline" @click="toggleSubscription(row.item)">
                    <bm-icon icon="trash" size="lg" />
                </bm-button>
            </template>
            <bm-button v-else variant="outline-neutral" @click="toggleSubscription(row.item)">
                <template v-if="isSubscribed(row.item)">{{ $t("common.unsubscribe") }}</template>
                <template v-else>{{ $t("common.subscribe") }}</template>
            </bm-button>
        </template>
    </bm-table>
</template>

<script>
import { ContainerType, isDefault, isManaged } from "./container";
import ManageMyContainerMenu from "./ManageMyContainerMenu";
import { ERROR, LOADING, SUCCESS } from "@bluemind/alert.store";
import { inject } from "@bluemind/inject";
import { BmButton, BmFormCheckbox, BmIcon, BmTable } from "@bluemind/styleguide";
import { retrieveTaskResult } from "@bluemind/task";
import { mapActions, mapState } from "vuex";
import { SAVE_ALERT } from "../../../Alerts/defaultAlerts";

export default {
    name: "ContainersManagementTable",
    components: { BmButton, BmFormCheckbox, BmIcon, BmTable, ManageMyContainerMenu },
    props: {
        containerType: {
            type: String,
            required: true
        },
        filtered: {
            type: Array,
            required: true
        },
        manageMine: {
            type: Boolean,
            required: true
        },
        shareColumn: {
            type: Boolean,
            required: true
        },
        perPage: {
            type: Number,
            required: true
        },
        currentPage: {
            type: Number,
            required: true
        },
        fieldId: {
            type: String,
            required: true
        }
    },
    data() {
        return { ContainerType, isDefault, isManaged, syncInProgress: {} };
    },
    computed: {
        ...mapState("preferences", ["subscriptions"]),
        sortBy() {
            return this.manageMine ? "defaultContainer" : "name";
        },
        sortDesc() {
            return this.manageMine;
        },
        defaultColumnTitle() {
            return this.$t("preferences.display_containers.default", {
                type: this.$tc("common.container_type." + this.containerType, 1)
            });
        },
        fields() {
            const fields = [
                { key: "name", sortable: true, label: this.$t("common.label"), class: "name" },
                { key: "offlineSync", label: this.$t("common.synchronization"), sortable: true },
                { key: "action", headerTitle: this.$t("common.action"), label: "", class: "text-right" }
            ];
            if (!this.manageMine) {
                fields.splice(1, 0, { key: "ownerDisplayname", headerTitle: this.$t("common.shared_by"), label: "" });
            }
            if (this.shareColumn) {
                fields.splice(2, 0, { key: "share", label: this.$t("common.sharing") });
            }
            if (this.manageMine) {
                fields.splice(0, 0, {
                    key: "defaultContainer",
                    headerTitle: this.defaultColumnTitle,
                    label: "",
                    class: "default"
                });
            }
            return fields;
        }
    },
    methods: {
        ...mapActions("alert", { ERROR, LOADING, SUCCESS }),
        ...mapActions("preferences", ["SUBSCRIBE_TO_CONTAINERS", "REMOVE_SUBSCRIPTIONS"]),
        isSubscribed(container) {
            return this.subscriptions.findIndex(sub => sub.value.containerUid === container.uid) !== -1;
        },
        openShareModal(container) {
            this.$emit("open-share-modal", container);
        },
        async remove(container) {
            const modalContent = this.$t("preferences.delete_containers", {
                type: this.$t("common.container_type_with_definite_article." + this.containerType)
            });
            const confirm = await this.$bvModal.msgBoxConfirm(modalContent, {
                title: this.$t("common.delete"),
                okTitle: this.$t("common.delete"),
                cancelTitle: this.$t("common.cancel"),
                okVariant: "secondary",
                cancelVariant: "simple-neutral",
                centered: true,
                hideHeaderClose: false,
                autoFocusButton: "ok"
            });
            if (confirm) {
                await this.REMOVE_SUBSCRIPTIONS([container.uid]);
                this.$emit("remove", container);
                this.SUCCESS(SAVE_ALERT);
            }
        },
        async resetData(container) {
            const confirm = await this.$bvModal.msgBoxConfirm(
                this.$t("preferences.reset_containers_data." + this.containerType),
                {
                    title: this.$t("common.action.empty"),
                    okTitle: this.$t("common.action.empty"),
                    cancelTitle: this.$t("common.cancel"),
                    okVariant: "secondary",
                    cancelVariant: "simple-neutral",
                    centered: true,
                    hideHeaderClose: false,
                    autoFocusButton: "ok"
                }
            );
            if (confirm) {
                this.$emit("reset-data", container);
                this.SUCCESS(SAVE_ALERT);
            }
        },
        async sync(container) {
            const ALERT = {
                alert: { name: "preferences.sync_calendar", uid: "SYNC_CALENDAR_UID" },
                options: { area: "pref-right-panel", renderer: "DefaultAlert" }
            };
            this.syncInProgress[container.uid] = true;
            this.LOADING(ALERT);
            const taskRef = await inject("ContainerSyncPersistence", container.uid).sync();
            const taskService = inject("TaskService", taskRef.id);
            try {
                await retrieveTaskResult(taskService);
                this.SUCCESS(ALERT);
            } catch {
                this.ERROR(ALERT);
            } finally {
                this.syncInProgress[container.uid] = false;
            }
        },
        async toggleOfflineSync(container) {
            const updatedContainer = { ...container, offlineSync: !container.offlineSync };
            await this.SUBSCRIBE_TO_CONTAINERS([updatedContainer]);
            if (this.containerType === ContainerType.MAILBOX && this.$route.path.startsWith("/mail/")) {
                this.$store.commit("preferences/fields/NEED_RELOAD", { id: this.fieldId });
            }
            this.$emit("offline-sync-changed", updatedContainer);
            this.SUCCESS(SAVE_ALERT);
        },
        async toggleSubscription(container) {
            if (this.isSubscribed(container)) {
                await this.REMOVE_SUBSCRIPTIONS([container.uid]);
                if (this.containerType === ContainerType.MAILBOX && this.$route.path.startsWith("/mail/")) {
                    this.$store.commit("preferences/fields/NEED_RELOAD", { id: this.fieldId });
                }
                if (!this.isManaged(container)) {
                    this.$emit("remove", container.uid);
                } else {
                    const updatedContainer = { ...container, offlineSync: false };
                    this.$emit("offline-sync-changed", updatedContainer);
                }
            } else {
                const updatedContainer = { ...container, offlineSync: true };
                await this.SUBSCRIBE_TO_CONTAINERS([updatedContainer]);
                if (this.containerType === ContainerType.MAILBOX && this.$route.path.startsWith("/mail/")) {
                    this.$store.commit("preferences/fields/NEED_RELOAD", { id: this.fieldId });
                }
                this.$emit("offline-sync-changed", updatedContainer);
            }
            this.SUCCESS(SAVE_ALERT);
        }
    }
};
</script>

<style>
.containers-management-table tbody td {
    vertical-align: middle;
}
</style>
