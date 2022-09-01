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
            <template #cell(icon)>
                <bm-icon icon="key" size="xl" />
            </template>
            <template #cell(displayName)="row">
                <div class="text-truncate" :title="row.value">
                    {{ row.value }}
                </div>
            </template>
            <template #cell(sid)="row">
                <div class="d-flex justify-content-between align-items-center">
                    <div class="sid-value text-truncate">
                        {{ row.value }}
                    </div>
                    <bm-button-copy variant="text" size="lg" :content-provider="() => row.value" />
                </div>
            </template>
            <template #cell(action)="row">
                <bm-icon-button variant="compact" icon="trash" @click="remove(row.item)" />
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
                    key: "icon",
                    label: "",
                    class: "icon-cell"
                },
                {
                    key: "displayName",
                    headerTitle: this.$t("common.label"),
                    label: "",
                    class: "label-cell"
                },
                {
                    key: "sid",
                    headerTitle: this.$t("preferences.security.api_key"),
                    label: "",
                    class: "sid-cell"
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
                okVariant: "contained-accent",
                cancelVariant: "text",
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

<style lang="scss">
@import "~@bluemind/styleguide/css/mixins/_responsiveness";
@import "~@bluemind/styleguide/css/_variables";

.pref-api-key {
    .b-table {
        margin-top: $sp-5;

        max-width: base-px-to-rem(800);
        table-layout: fixed;

        thead {
            display: none;
        }
    }
    .icon-cell {
        width: base-px-to-rem(10);
        .bm-icon {
            display: none;
        }
        @include from-lg {
            width: base-px-to-rem(54);
            padding-top: base-px-to-rem(2) !important;
            .bm-icon {
                display: inline-block;
                color: $neutral-fg-lo2;
            }
        }
    }
    .label-cell {
        width: 70%;
    }
    .sid-cell {
        width: 100%;
        .sid-value {
            flex: 0 1 base-px-to-rem(330);
        }
        .bm-button-copy {
            flex: none;
        }
    }
    .action-cell {
        width: base-px-to-rem(50);
    }
}
</style>
