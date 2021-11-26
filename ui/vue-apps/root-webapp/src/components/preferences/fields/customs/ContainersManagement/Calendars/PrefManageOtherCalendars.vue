<template>
    <containers-management
        :containers="otherCalendars"
        :container-type="containerType"
        @offline-sync-changed="UPDATE_OTHER_CALENDAR"
        @remove="REMOVE_OTHER_CALENDAR"
        @subscribe="ADD_OTHER_CALENDARS"
        @update="UPDATE_OTHER_CALENDAR"
    >
        <template v-slot:item="{ container }"><bm-calendar-item :calendar="container" /></template>
        <template v-slot:badge-item="{ container, closeFn }">
            <bm-calendar-badge :calendar="container" closeable @close="closeFn(container)" />
        </template>
    </containers-management>
</template>

<script>
import BmCalendarBadge from "./BmCalendarBadge";
import BmCalendarItem from "./BmCalendarItem";
import { ContainerType } from "../container";
import ContainersManagement from "../ContainersManagement";
import { mapMutations, mapState } from "vuex";

export default {
    name: "PrefManageOtherCalendars",
    components: { BmCalendarBadge, BmCalendarItem, ContainersManagement },
    data() {
        return { containerType: ContainerType.CALENDAR };
    },
    computed: {
        ...mapState("preferences", { otherCalendars: state => state.preferenceContainers.otherCalendars })
    },
    methods: {
        ...mapMutations("preferences", ["ADD_OTHER_CALENDARS", "REMOVE_OTHER_CALENDAR", "UPDATE_OTHER_CALENDAR"])
    }
};
</script>
