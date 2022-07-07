<template>
    <containers-management
        class="pref-manage-my-calendars"
        :containers="myCalendars"
        :container-type="containerType"
        :create-container-fn="create"
        :collapsed="collapsed"
        :field-id="id"
        manage-mine
        @offline-sync-changed="UPDATE_PERSONAL_CALENDAR"
        @update="update"
        @remove="remove"
        @reset-data="resetData"
    >
        <template v-slot:item="{ container }"><bm-calendar-item :calendar="container" /></template>
    </containers-management>
</template>

<script>
import { mapActions, mapMutations, mapState } from "vuex";

import { inject } from "@bluemind/inject";
import { SUCCESS } from "@bluemind/alert.store";

import { containerToCalendarDescriptor, ContainerHelper, ContainerType } from "../../container";
import CalendarHelper from "../helper";
import BaseField from "../../../../../mixins/BaseField";
import BmCalendarItem from "../BmCalendarItem";
import ContainersManagement from "../../ContainersManagement";
import { SAVE_ALERT } from "../../../../../Alerts/defaultAlerts";

ContainerHelper.register(ContainerType.CALENDAR, CalendarHelper);

export default {
    name: "PrefManageMyCalendars",
    components: { BmCalendarItem, ContainersManagement },
    mixins: [BaseField],
    data() {
        return { containerType: ContainerType.CALENDAR };
    },
    computed: {
        ...mapState("preferences", { myCalendars: state => state.containers.myCalendars })
    },
    methods: {
        ...mapActions("preferences", ["SUBSCRIBE_TO_CONTAINERS"]),
        ...mapActions("alert", { SUCCESS }),
        ...mapMutations("preferences", [
            "ADD_PERSONAL_CALENDAR",
            "REMOVE_PERSONAL_CALENDAR",
            "UPDATE_PERSONAL_CALENDAR"
        ]),
        resetData(calendar) {
            inject("CalendarPersistence", calendar.uid).reset();
        },
        async remove(container) {
            await inject("CalendarsMgmtPersistence").remove(container.uid);
            this.REMOVE_PERSONAL_CALENDAR(container.uid);
            this.SUCCESS(SAVE_ALERT);
        },
        async create(container) {
            const calendarDescriptor = containerToCalendarDescriptor(container);

            await inject("CalendarsMgmtPersistence").create(container.uid, calendarDescriptor);
            if (container.settings.bm_color) {
                inject("ContainerManagementPersistence", container.uid).setPersonalSettings({
                    bm_color: container.settings.bm_color
                });
            }
            this.ADD_PERSONAL_CALENDAR(container);
            this.SUBSCRIBE_TO_CONTAINERS([container]);
            this.SUCCESS(SAVE_ALERT);
        },
        async update(container) {
            const calendarDescriptor = containerToCalendarDescriptor(container);
            await inject("CalendarsMgmtPersistence").update(container.uid, calendarDescriptor);
            if (container.settings.bm_color) {
                inject("ContainerManagementPersistence", container.uid).setPersonalSettings({
                    bm_color: container.settings.bm_color
                });
            }
            this.UPDATE_PERSONAL_CALENDAR(container);
            this.SUCCESS(SAVE_ALERT);
        }
    }
};
</script>

<style lang="scss">
.pref-manage-my-calendars {
    .name {
        width: 60%;
    }
    .default {
        width: 10%;
    }
}
</style>
