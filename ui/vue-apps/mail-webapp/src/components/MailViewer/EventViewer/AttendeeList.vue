<script setup>
import { Contact } from "@bluemind/business-components";
import MailContactCardSlots from "../../MailContactCardSlots";
import EventFooterSection from "./base/EventFooterSection.vue";

defineProps({
    label: { type: String, required: true },
    attendees: { type: Array, required: true }
});
</script>

<template>
    <event-footer-section v-if="attendees?.length" :label="label" role="list">
        <div v-for="(attendee, index) in attendees" :key="index" class="event-footer-entry" role="listitem">
            <mail-contact-card-slots
                :component="Contact"
                :contact="attendee"
                no-avatar
                show-address
                transparent
                bold-dn
                enable-card
                class="text-truncate"
            />
        </div>
    </event-footer-section>
</template>
