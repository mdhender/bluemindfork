<template>
    <containers-management
        :containers="otherCalendars"
        :container-type="containerType"
        :field-id="id"
        :read-more="readMore"
        @offline-sync-changed="UPDATE_OTHER_CALENDAR"
        @remove="REMOVE_OTHER_CALENDAR"
        @subscribe="ADD_OTHER_CALENDARS"
        @update="UPDATE_OTHER_CALENDAR"
    >
        <template #item="{ container }"><bm-calendar-item :calendar="container" /></template>
        <template #badge-item="{ container, closeFn }">
            <bm-calendar-badge :calendar="container" closeable @close="closeFn(container)" />
        </template>
    </containers-management>
</template>

<script>
import BmCalendarBadge from "./BmCalendarBadge";
import BmCalendarItem from "./BmCalendarItem";
import { ContainerHelper, ContainerType } from "../container";
import CalendarHelper from "./helper";
import { BaseField } from "@bluemind/preferences";
import ContainersManagement from "../ContainersManagement";
import { mapMutations, mapState } from "vuex";

ContainerHelper.register(ContainerType.CALENDAR, CalendarHelper);

export default {
    name: "PrefManageOtherCalendars",
    components: { BmCalendarBadge, BmCalendarItem, ContainersManagement },
    mixins: [BaseField],
    data() {
        return {
            containerType: ContainerType.CALENDAR,
            readMore: {
                href: "https://doc.bluemind.net/release/5.1/guide_de_l_utilisateur/l_agenda/utiliser_un_calendrier_partage",
                text: this.$t("preferences.display_containers.other_calendars.read_more")
            }
        };
    },
    computed: {
        ...mapState("preferences", { otherCalendars: state => state.containers.otherCalendars })
    },
    methods: {
        ...mapMutations("preferences", ["ADD_OTHER_CALENDARS", "REMOVE_OTHER_CALENDAR", "UPDATE_OTHER_CALENDAR"])
    }
};
</script>
