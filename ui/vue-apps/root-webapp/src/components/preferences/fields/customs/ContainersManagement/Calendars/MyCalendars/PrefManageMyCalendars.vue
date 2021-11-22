<template>
    <containers-management
        class="pref-manage-my-calendars"
        :containers="myCalendars"
        :container-type="containerType"
        manage-mine
        @create="create"
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

import { containerToCalendarDescriptor, ContainerType } from "../../container";
import BmCalendarItem from "../BmCalendarItem";
import ContainersManagement from "../../ContainersManagement";

export default {
    name: "PrefManageMyCalendars",
    components: { BmCalendarItem, ContainersManagement },
    data() {
        return { containerType: ContainerType.CALENDAR };
    },
    computed: {
        ...mapState("preferences", ["myCalendars"])
    },
    methods: {
        ...mapActions("preferences", ["SUBSCRIBE_TO_CONTAINERS"]),
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
        }
    }
};
</script>
