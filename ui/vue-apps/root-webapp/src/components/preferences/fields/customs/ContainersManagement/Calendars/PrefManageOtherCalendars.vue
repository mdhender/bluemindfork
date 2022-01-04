<template>
    <containers-management
        :containers="otherCalendars"
        :container-type="containerType"
        :collapsed="collapsed"
        :field-id="id"
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
import { ContainerHelper, ContainerType } from "../container";
import CalendarHelper from "./helper";
import BaseField from "../../../../mixins/BaseField";
import ContainersManagement from "../ContainersManagement";
import { mapMutations, mapState } from "vuex";

ContainerHelper.register(ContainerType.CALENDAR, CalendarHelper);

export default {
    name: "PrefManageOtherCalendars",
    components: { BmCalendarBadge, BmCalendarItem, ContainersManagement },
    mixins: [BaseField],
    data() {
        return { containerType: ContainerType.CALENDAR };
    },
    computed: {
        ...mapState("preferences", { otherCalendars: state => state.containers.otherCalendars })
    },
    methods: {
        ...mapMutations("preferences", ["ADD_OTHER_CALENDARS", "REMOVE_OTHER_CALENDAR", "UPDATE_OTHER_CALENDAR"])
    }
};
</script>
