<template>
    <bm-modal
        v-model="show"
        centered
        lazy
        :title="modalTitle()"
        :cancel-title="$t('common.cancel')"
        :ok-title="isNew ? $t('common.create') : $t('common.save')"
        :ok-disabled="disableSave"
        modal-class="create-or-update-container-modal"
        body-class="row mt-3"
        @ok.prevent="save"
    >
        <bm-form class="w-100" @submit.prevent="save">
            <bm-form-group
                :label="$t('common.label')"
                label-for="label"
                :description="labelDesc()"
                :invalid-feedback="$t('preferences.create_container.name_already_exists')"
                :state="isLabelValid"
            >
                <bm-form-input
                    id="label"
                    v-model.trim="container.name"
                    type="text"
                    required
                    :icon="containerIcon"
                    left-icon
                    :disabled="isDefault"
                    :state="container.name ? isLabelValid : null"
                    autofocus
                />
            </bm-form-group>
            <create-or-update-calendar
                v-if="isCalendarType"
                v-model="container"
                :is-default="isDefault"
                :is-new="isNew"
                @is-valid="isValid => (isCalValid = isValid)"
            />
        </bm-form>
        <import-file v-if="showFileImport" ref="import-file" :container="container" class="mt-2 flex-grow-1" />
    </bm-modal>
</template>

<script>
import { ContainerHelper, ContainerType } from "./container";
import CreateOrUpdateCalendar from "./Calendars/MyCalendars/CreateOrUpdateCalendar";
import ImportFile from "./ImportFile";
import { WARNING, SUCCESS } from "@bluemind/alert.store";
import { BmForm, BmFormGroup, BmFormInput, BmModal } from "@bluemind/ui-components";
import UUIDGenerator from "@bluemind/uuid";
import cloneDeep from "lodash.clonedeep";
import { mapActions } from "vuex";
import { SAVE_ALERT } from "../../../Alerts/defaultAlerts";

export default {
    name: "CreateOrUpdateContainerModal",
    components: { BmForm, BmFormGroup, BmFormInput, BmModal, CreateOrUpdateCalendar, ImportFile },
    props: {
        containers: {
            type: Array,
            required: true
        },
        createFn: {
            type: Function,
            required: true
        }
    },
    data() {
        return {
            show: false,
            container: {},
            originalContainer: {},
            isCalValid: true,
            isNew: true,
            actionsInProgress: false
        };
    },
    computed: {
        isCalendarType() {
            return this.container.type === ContainerType.CALENDAR;
        },
        containerIcon() {
            return this.container.type ? ContainerHelper.use(this.container.type).matchingIcon() : "";
        },
        disableSave() {
            return !this.anyChange || this.isInvalid || this.actionsInProgress;
        },
        isInvalid() {
            return (this.isCalendarType && !this.isCalValid) || !this.isLabelValid;
        },
        isDefault() {
            return Boolean(this.container.uid) && this.container.defaultContainer;
        },
        anyChange() {
            return JSON.stringify(this.container) !== JSON.stringify(this.originalContainer);
        },
        isLabelValid() {
            return this.container.name && !this.nameAlreadyExists;
        },
        nameAlreadyExists() {
            return (
                this.containers.findIndex(
                    existing => existing.uid !== this.container.uid && existing.name === this.container.name
                ) !== -1
            );
        },
        showFileImport() {
            return this.isNew && (!this.isCalendarType || this.container.settings.type === "internal");
        }
    },
    methods: {
        ...mapActions("alert", { WARNING, SUCCESS }),
        async open(container) {
            this.originalContainer = container;
            this.container = cloneDeep(container);
            this.isNew = !this.container.uid;
            this.show = true;
        },
        async save() {
            this.actionsInProgress = true;
            if (this.isNew) {
                await this.create();
            } else {
                this.$emit("update", this.container);
            }
            this.show = false;
            this.SUCCESS(SAVE_ALERT);
            this.actionsInProgress = false;
        },
        async create() {
            const uid = UUIDGenerator.generate();
            await this.createFn({ ...this.container, uid });
            try {
                await this.$refs["import-file"].uploadFile(uid);
                this.SUCCESS(SAVE_ALERT);
            } catch (e) {
                this.WARNING({
                    alert: { name: "preferences.containers.create_and_import_data", uid: "IMPORT_DATA_UID" },
                    options: { area: "pref-right-panel", renderer: "DefaultAlert" }
                });
            }
        },

        modalTitle() {
            if (this.container.type) {
                return this.isNew
                    ? this.$t("preferences.create_container." + this.container.type + ".button")
                    : this.$t("preferences.update_container." + this.container.type + ".button", {
                          name: this.container.name
                      });
            }
        },
        labelDesc() {
            if (this.container.type) {
                return this.$t("preferences.create_container.label.description", {
                    type: this.$tc("common.container_type." + this.container.type, 1)
                });
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.create-or-update-container-modal .fa-calendar {
    color: $neutral-fg;
}
</style>
