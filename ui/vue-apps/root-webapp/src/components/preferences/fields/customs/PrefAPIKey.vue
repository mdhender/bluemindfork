<template>
    <div class="pref-api-key">
        <div class="mb-2">{{ $t("preferences.security.api_key.desc") }}</div>
        <bm-button variant="outline" size="lg" icon="plus" @click="openModal">
            {{ $t("preferences.security.api_key.generate") }}
        </bm-button>

        <bm-modal
            v-model="showModal"
            :title="$t('preferences.security.api_key.generate')"
            :ok-title="$t('preferences.security.api_key.generate.modal.ok_button')"
            :ok-disabled="!projectLabelValid"
            :cancel-title="$t('common.cancel')"
            centered
            @ok="generateAPIKey"
        >
            <bm-form class="mt-4" @submit.prevent="generateAndClose">
                <bm-form-group
                    :label="$t('preferences.security.api_key.generate.modal.project_label')"
                    label-for="project-label"
                >
                    <bm-form-input id="project-label" ref="project-label-input" v-model="projectLabel" autofocus />
                </bm-form-group>
            </bm-form>
        </bm-modal>

        <bm-table :items="keys" :fields="fields" :per-page="perPage" :current-page="currentPage" sort-by="displayName">
            <template #cell(displayName)="row">
                <bm-icon class="text-secondary mr-2" icon="key" size="xl" /> {{ row.value }}
            </template>
            <template #cell(sid)="row">
                <div class="d-flex justify-content-between align-items-center">
                    {{ row.value }}
                    <bm-button-copy variant="text" size="lg" class="ml-4" :content-provider="() => row.value" />
                </div>
            </template>
            <template #cell(action)="row">
                <bm-icon-button size="sm" icon="trash" @click="remove(row.item)" />
            </template>
        </bm-table>
        <bm-pagination v-model="currentPage" :total-rows="totalRows" :per-page="perPage" />
    </div>
</template>

<script>
import { mapActions } from "vuex";
import {
    BmButton,
    BmButtonCopy,
    BmIconButton,
    BmForm,
    BmFormGroup,
    BmFormInput,
    BmIcon,
    BmModal,
    BmPagination,
    BmTable
} from "@bluemind/styleguide";
import { inject } from "@bluemind/inject";
import { SUCCESS } from "@bluemind/alert.store";
import { SAVE_ALERT } from "../../Alerts/defaultAlerts";

export default {
    name: "PrefAPIKey",
    components: {
        BmButton,
        BmButtonCopy,
        BmIconButton,
        BmForm,
        BmFormGroup,
        BmFormInput,
        BmIcon,
        BmModal,
        BmPagination,
        BmTable
    },
    data() {
        return {
            showModal: false,
            projectLabel: "",
            keys: [],

            currentPage: 1,
            perPage: 5,
            fields: [
                {
                    key: "displayName",
                    headerTitle: this.$t("common.label"),
                    label: "",
                    class: "w-50 align-middle"
                },
                {
                    key: "sid",
                    headerTitle: this.$t("preferences.security.api_key"),
                    label: "",
                    class: "text-nowrap"
                },
                {
                    key: "action",
                    headerTitle: this.$t("common.action"),
                    label: "",
                    class: "text-right w-50"
                }
            ]
        };
    },
    computed: {
        projectLabelValid() {
            return this.projectLabel.trim().length > 0;
        },
        totalRows() {
            return this.keys.length;
        }
    },
    async created() {
        this.keys = await inject("APIKeysPersistence").list();
    },
    methods: {
        ...mapActions("alert", { SUCCESS }),
        async generateAPIKey() {
            const key = await inject("APIKeysPersistence").create(this.projectLabel);
            this.keys.push(key);
            this.SUCCESS(SAVE_ALERT);
        },
        generateAndClose() {
            if (this.projectLabelValid) {
                this.generateAPIKey();
                this.showModal = false;
            }
        },
        async remove(key) {
            const modalContent = this.$t("preferences.security.api_key.remove", { name: key.displayName });
            const confirm = await this.$bvModal.msgBoxConfirm(modalContent, {
                title: this.$t("common.delete"),
                okTitle: this.$t("common.delete"),
                okVariant: "secondary",
                cancelVariant: "outline-neutral",
                cancelTitle: this.$t("common.cancel"),
                centered: true,
                hideHeaderClose: false,
                autoFocusButton: "ok"
            });
            if (confirm) {
                await inject("APIKeysPersistence").remove(key.sid);
                const index = this.keys.findIndex(k => k.sid === key.sid);
                this.keys.splice(index, 1);
                this.SUCCESS(SAVE_ALERT);
            }
        },
        openModal() {
            this.projectLabel = "";
            this.showModal = true;
        }
    }
};
</script>
