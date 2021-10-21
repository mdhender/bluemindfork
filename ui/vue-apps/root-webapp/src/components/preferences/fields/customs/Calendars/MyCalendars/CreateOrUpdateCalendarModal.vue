<template>
    <bm-modal
        v-model="show"
        centered
        lazy
        :title="
            isNew
                ? $t('preferences.calendar.my_calendars.new')
                : $t('preferences.calendar.my_calendars.update', { calendarName: label })
        "
        :cancel-title="$t('common.cancel')"
        :ok-title="isNew ? $t('common.create') : $t('common.save')"
        :ok-disabled="disableSave"
        body-class="row update-calendar-modal-body mt-3"
        @ok="save"
    >
        <bm-col cols="2"><bm-icon icon="calendar" size="3x" class="mt-3" /></bm-col>
        <bm-col cols="10">
            <bm-form>
                <bm-form-group
                    :label="$t('common.label')"
                    label-for="calendar-label"
                    :description="$t('preferences.calendar.my_calendars.label.description')"
                >
                    <bm-form-input
                        id="calendar-label"
                        v-model="label"
                        type="text"
                        required
                        :disabled="isDefaultCalendar"
                    />
                </bm-form-group>
                <bm-form-group label-for="calendar-type" :label="$t('preferences.calendar.my_calendars.type')">
                    <!-- FIXME: z-index problem on this select which is shown under modal footer -->
                    <bm-form-select
                        v-model="type"
                        :options="possibleTypes"
                        class="w-100"
                        :disabled="isDefaultCalendar"
                    />
                </bm-form-group>
                <bm-form-group
                    v-if="type === 'externalIcs'"
                    :label="$t('common.external_ics.url')"
                    label-for="calendar-ics-url"
                >
                    <bm-form-input
                        id="calendar-ics-url"
                        v-model="icsUrl"
                        type="text"
                        class="mb-1"
                        required
                        @blur="checkIcsUrlValidity"
                    />
                    <bm-spinner v-if="icsUrlValidityStatus === 'LOADING'" :size="0.2" />
                    <template v-if="icsUrlValidityStatus === 'VALID'">
                        <bm-icon icon="check-circle" size="lg" class="text-success" />
                        {{ $t("common.valid_url") }}
                    </template>
                    <template v-if="icsUrlValidityStatus === 'ERROR'">
                        <bm-icon icon="exclamation-circle" size="lg" class="text-danger" />
                        {{ $t("common.invalid_url") }}
                    </template>
                </bm-form-group>
                <bm-form-group :label="$t('common.color')" label-for="calendar-color">
                    <bm-form-color-picker id="calendar-color" v-model="color" type="text" required />
                </bm-form-group>
            </bm-form>
        </bm-col>
    </bm-modal>
</template>

<script>
import calendarToSubscription from "../calendarToSubscription";
import { inject } from "@bluemind/inject";
import {
    BmCol,
    BmForm,
    BmFormColorPicker,
    BmFormGroup,
    BmFormInput,
    BmFormSelect,
    BmIcon,
    BmModal,
    BmSpinner
} from "@bluemind/styleguide";
import UUIDGenerator from "@bluemind/uuid";
import { mapActions, mapMutations } from "vuex";

export default {
    name: "CreateOrUpdateCalendarModal",
    components: {
        BmCol,
        BmForm,
        BmFormColorPicker,
        BmFormGroup,
        BmFormInput,
        BmFormSelect,
        BmIcon,
        BmModal,
        BmSpinner
    },
    data() {
        return {
            show: false,

            isNew: false,
            originalCalendar: {},

            label: "",
            type: "internal",
            icsUrl: "",
            color: "",

            icsUrlValidityStatus: "IDLE",

            possibleTypes: [
                { text: this.$t("common.simple"), value: "internal" },
                { text: this.$t("common.external_ics"), value: "externalIcs" }
            ]
        };
    },
    computed: {
        disableSave() {
            const newCalendarFormIsValid =
                !this.label || (this.type === "externalIcs" && this.icsUrlValidityStatus !== "VALID");
            const noModificationOnUpdated =
                !this.isNew &&
                !this.anyChangeOnExistingCalendar &&
                this.color === this.originalCalendar.settings.bm_color;
            return newCalendarFormIsValid || noModificationOnUpdated;
        },
        isDefaultCalendar() {
            return (
                !this.isNew &&
                this.originalCalendar.uid &&
                this.originalCalendar.uid === "calendar:Default:" + inject("UserSession").userId
            );
        },
        anyChangeOnExistingCalendar() {
            return (
                !this.isNew &&
                !this.isDefaultCalendar &&
                (this.label !== this.originalCalendar.name ||
                    this.type !== this.originalCalendar.settings.type ||
                    (this.type === "externalIcs" && this.icsUrl !== this.originalCalendar.settings.icsUrl))
            );
        }
    },
    methods: {
        ...mapActions("preferences", ["SET_SUBSCRIPTIONS"]),
        ...mapMutations("preferences", ["ADD_PERSONAL_CALENDAR", "UPDATE_PERSONAL_CALENDAR"]),
        async open(calendar) {
            if (!calendar) {
                this.isNew = true;
                this.label = "";
                this.type = "internal";
                this.icsUrl = "";
                this.color = "";
                this.icsUrlValidityStatus = "IDLE";
            } else {
                this.originalCalendar = { ...calendar };
                this.isNew = false;
                this.label = calendar.name;
                this.type = calendar.settings.type || "internal";
                this.icsUrl = calendar.settings.icsUrl;
                this.color = calendar.settings.bm_color;
                this.icsUrlValidityStatus = calendar.settings.icsUrl ? "VALID" : "IDLE";
            }
            this.show = true;
        },
        save() {
            const userSession = inject("UserSession");
            const settings = {
                type: this.type
            };
            if (this.type === "externalIcs") {
                settings.readonly = "true";
                settings.icsUrl = this.icsUrl;
            }
            if (this.color) {
                settings.bm_color = this.color;
            }
            const calendarDesc = {
                domainUid: userSession.domain,
                name: this.label,
                owner: userSession.userId,
                settings
            };

            if (this.isNew) {
                this.createCalendar(userSession, calendarDesc);
            } else {
                this.updateCalendar(calendarDesc);
            }
        },
        async createCalendar(userSession, calendarDesc) {
            const uid = UUIDGenerator.generate();

            await inject("CalendarsMgmtPersistence").create(uid, calendarDesc);
            if (this.color) {
                inject("ContainerManagementPersistence", uid).setPersonalSettings({ bm_color: this.color });
            }

            this.ADD_PERSONAL_CALENDAR({
                uid,
                name: calendarDesc.name,
                owner: userSession.userId,
                type: "calendar",
                defaultContainer: false,
                readOnly: false,
                domainUid: userSession.domain,
                ownerDisplayname: userSession.formatedName,
                ownerDirEntryPath: userSession.domain + "/users/" + userSession.userId,
                settings: calendarDesc.settings,
                deleted: false
            });
            const subscription = calendarToSubscription(userSession, { uid, name: this.label, offlineSync: true });
            this.SET_SUBSCRIPTIONS([subscription]);
        },
        async updateCalendar(calendarDesc) {
            if (this.anyChangeOnExistingCalendar) {
                await inject("CalendarsMgmtPersistence").update(this.originalCalendar.uid, calendarDesc);
            }
            if (this.color !== this.originalCalendar.settings.bm_color) {
                inject("ContainerManagementPersistence", this.originalCalendar.uid).setPersonalSettings({
                    bm_color: this.color
                });
            }
            this.UPDATE_PERSONAL_CALENDAR({
                ...this.originalCalendar,
                name: calendarDesc.name,
                settings: calendarDesc.settings
            });
        },
        checkIcsUrlValidity() {
            this.icsUrlValidityStatus = "LOADING";
            try {
                new URL(this.icsUrl);
                fetch("calendar/checkIcs?url=" + this.icsUrl).then(res => {
                    if (res.status !== 200) {
                        this.icsUrlValidityStatus = "ERROR";
                    } else {
                        this.icsUrlValidityStatus = "VALID";
                    }
                });
            } catch (e) {
                this.icsUrlValidityStatus = "ERROR";
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.update-calendar-modal-body .fa-event {
    color: $cyan;
}
</style>
