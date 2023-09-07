<script setup>
import { BmButtonExpand } from "@bluemind/ui-components";
import { computed, ref } from "vue";

const props = defineProps({ event: { type: Object, required: true } });

const attendees = computed(() => props.event?.attendees ?? []);
const showFooter = computed(() => attendees.value.length);

const showAttendees = ref(false);
</script>

<template>
    <div v-if="showFooter" class="event-footer">
        <div class="attendees-header">
            <bm-button-expand :expanded="showAttendees" @click.prevent="showAttendees = !showAttendees" />
            <span class="font-weight-bold">
                {{ $tc("mail.viewer.invitation.attendee", attendees.length, { count: attendees.length }) }}
            </span>
        </div>
        <div v-if="showAttendees" class="attendees-body">
            <div v-for="attendee in attendees" :key="attendee.mail" class="attendee">
                <span class="font-weight-bold">{{ attendee.name }}</span>
                <span class="text-truncate" v-text="`<${attendee.mail}>`" />
            </div>
        </div>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-footer {
    padding: 0 $sp-5 0 $sp-2;
    display: flex;
    flex-direction: column;
    gap: $sp-4;

    .attendees-header {
        display: flex;
        align-items: center;
    }
    .attendees-body {
        display: flex;
        flex-direction: column;
        gap: $sp-3;
        padding: 0 0 $sp-2 $sp-6;
        .attendee {
            margin-left: $sp-2;
            display: flex;
            color: $neutral-fg;
            gap: $sp-4;
            line-height: $line-height-sm;
        }
    }
}
</style>
