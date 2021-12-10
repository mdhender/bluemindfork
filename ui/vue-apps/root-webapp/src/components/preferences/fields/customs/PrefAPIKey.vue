<template>
    <div class="pref-api-key">
        <div class="mb-2">{{ $t("preferences.security.api_key.desc") }}</div>
        <bm-button variant="outline-secondary" @click="openModal">
            <bm-icon icon="plus" />
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
                <bm-icon class="text-primary mr-2" icon="key" size="lg" /> {{ row.value }}
            </template>
            <template #cell(sid)="row">
                <div class="d-flex justify-content-between align-items-center">
                    {{ row.value }}
                    <bm-button
                        :variant="lastCopiedSid === row.value ? 'success' : 'outline-secondary'"
                        class="ml-4"
                        @click="copySid(row.value)"
                    >
                        <bm-icon :icon="lastCopiedSid === row.value ? 'check' : 'copy'" />
                        <span class="pl-1">
                            {{ lastCopiedSid === row.value ? $t("common.copied") : $t("common.copy") }}
                        </span>
                    </bm-button>
                </div>
            </template>
            <template #cell(action)="row">
                <bm-button variant="inline-secondary" @click="remove(row.item)">
                    <bm-icon icon="trash" size="lg" />
                </bm-button>
            </template>
        </bm-table>
        <bm-pagination v-model="currentPage" :total-rows="totalRows" :per-page="perPage" />
    </div>
</template>

<script>
import {
    BmButton,
    BmForm,
    BmFormGroup,
    BmFormInput,
    BmIcon,
    BmModal,
    BmPagination,
    BmTable
} from "@bluemind/styleguide";
import { inject } from "@bluemind/inject";

export default {
    name: "PrefAPIKey",
    components: { BmButton, BmForm, BmFormGroup, BmFormInput, BmIcon, BmModal, BmPagination, BmTable },
    data() {
        return {
            showModal: false,
            projectLabel: "",
            keys: [],
            lastCopiedSid: -1,

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
        async generateAPIKey() {
            const key = await inject("APIKeysPersistence").create(this.projectLabel);
            this.keys.push(key);
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
                cancelVariant: "outline-secondary",
                cancelTitle: this.$t("common.cancel"),
                centered: true,
                hideHeaderClose: false,
                autoFocusButton: "ok"
            });
            if (confirm) {
                await inject("APIKeysPersistence").remove(key.sid);
                const index = this.keys.findIndex(k => k.sid === key.sid);
                this.keys.splice(index, 1);
            }
        },
        copySid(sid) {
            navigator.clipboard.writeText(sid);
            this.lastCopiedSid = sid;
        },
        openModal() {
            this.projectLabel = "";
            this.showModal = true;
        }
    }
};
</script>
