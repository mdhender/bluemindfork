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
        @ok="save"
    >
        <div class="col-2"><bm-icon :icon="containerIcon" size="3x" class="mt-3" /></div>
        <bm-form class="col-10" @submit.prevent="save">
            <bm-form-group :label="$t('common.label')" label-for="label" :description="labelDesc()">
                <bm-form-input
                    id="label"
                    v-model="container.name"
                    type="text"
                    required
                    :disabled="isDefault"
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
    </bm-modal>
</template>

<script>
import { ContainerType, isDefault, matchingIcon } from "./container";
import CreateOrUpdateCalendar from "./Calendars/MyCalendars/CreateOrUpdateCalendar";
import { BmForm, BmFormGroup, BmFormInput, BmIcon, BmModal } from "@bluemind/styleguide";
import UUIDGenerator from "@bluemind/uuid";
import cloneDeep from "lodash.clonedeep";

export default {
    name: "CreateOrUpdateContainerModal",
    components: { BmForm, BmFormGroup, BmFormInput, BmIcon, BmModal, CreateOrUpdateCalendar },
    props: {
        containers: {
            type: Array,
            required: true
        }
    },
    data() {
        return { show: false, container: {}, originalContainer: {}, isCalValid: true };
    },
    computed: {
        isNew() {
            return !this.container.uid;
        },
        isCalendarType() {
            return this.container.type === ContainerType.CALENDAR;
        },
        containerIcon() {
            return matchingIcon(this.container.type);
        },
        disableSave() {
            return !this.anyChange || this.isInvalid;
        },
        isInvalid() {
            return (this.isCalendarType && !this.isCalValid) || !this.isLabelValid;
        },
        isDefault() {
            return Boolean(this.container.uid) && isDefault(this.container.uid);
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
        }
    },
    methods: {
        async open(container) {
            this.originalContainer = container;
            this.container = cloneDeep(container);
            this.show = true;
        },
        save() {
            if (this.isNew) {
                const uid = UUIDGenerator.generate();
                this.$emit("create", { ...this.container, uid });
            } else {
                this.$emit("update", this.container);
            }
            this.show = false;
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
@import "~@bluemind/styleguide/css/_variables";

.create-or-update-container-modal .fa-calendar {
    color: $calendar-color;
}
</style>
