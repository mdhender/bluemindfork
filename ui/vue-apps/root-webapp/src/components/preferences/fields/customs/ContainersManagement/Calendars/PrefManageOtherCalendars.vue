<template>
    <containers-management
        :containers="otherCalendars"
        :container-type="containerType"
        :has-share-column="false"
        @add="ADD_OTHER_CALENDARS"
        @update="UPDATE_OTHER_CALENDAR"
        @remove="REMOVE_OTHER_CALENDAR"
    >
        <template v-slot:item="{ container }"><bm-calendar-item :calendar="container" /></template>
        <template v-slot:badge-item="{ container, closeFn }">
            <bm-calendar-badge :calendar="container" closeable @close="closeFn(container)" />
        </template>
        <template v-slot:action="{ container, isManaged, openShareModal, toggleSubscription }">
            <bm-button v-if="isManaged(container)" variant="inline" @click="openShareModal(container)">
                <bm-icon icon="share" size="lg" />
            </bm-button>
            <bm-button v-else variant="inline" @click="toggleSubscription(container)">
                <bm-icon icon="trash" size="lg" />
            </bm-button>
        </template>
    </containers-management>
</template>

<script>
import BmCalendarBadge from "./BmCalendarBadge";
import BmCalendarItem from "./BmCalendarItem";
import { ContainerType } from "../container";
import ContainersManagement from "../ContainersManagement";
import { BmButton, BmIcon } from "@bluemind/styleguide";
import { mapMutations, mapState } from "vuex";

export default {
    name: "PrefManageOtherCalendars",
    components: { BmButton, BmCalendarBadge, BmCalendarItem, BmIcon, ContainersManagement },
    data() {
        return { containerType: ContainerType.CALENDAR };
    },
    computed: {
        ...mapState("preferences", ["otherCalendars"])
    },
    methods: {
        ...mapMutations("preferences", ["ADD_OTHER_CALENDARS", "REMOVE_OTHER_CALENDAR", "UPDATE_OTHER_CALENDAR"])
    }
};
</script>
