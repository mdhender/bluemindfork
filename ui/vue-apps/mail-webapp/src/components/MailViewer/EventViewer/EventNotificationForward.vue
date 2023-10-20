<script setup>
import { computed } from "vue";
import { BmToggleableButton, BmDropdown, BmDropdownItem, BmLabelIcon } from "@bluemind/ui-components";
import { Contact } from "@bluemind/business-components";
import MailContactCardSlots from "../../MailContactCardSlots";
import EventHeader from "./base/EventHeader";
import EventDetail from "./base/EventDetail";
import EventFooter from "./base/EventFooter";
import EventFooterSection from "./base/EventFooterSection";
import { messageUtils } from "@bluemind/mail";

const { MessageHeader } = messageUtils;

const props = defineProps({
    message: { type: Object, required: true },
    event: { type: Object, required: true }
});
const attendeeHeader = computed(() =>
    props.message?.headers?.find(({ name }) => name.toUpperCase() === MessageHeader.X_BM_COUNTER_ATTENDEE.toUpperCase())
);
const newlyAddedAttendees = computed(() =>
    props.event.attendees
        ?.filter(a => attendeeHeader.value.values?.[0]?.includes(a.mail))
        ?.map(a => ({ address: a.mail, dn: a.name }))
);
const emit = defineEmits(["rejects"]);
function rejectAttendees(attendees) {
    emit("rejectAttendee", attendees);
}
</script>

<template>
    <div v-if="attendeeHeader" class="event-countered">
        <event-header>
            <span class="bold"> {{ $t("mail.viewer.invitation.counter.attendees") }}</span>
            <template #actions>
                <bm-dropdown
                    v-if="newlyAddedAttendees?.length > 1 && newlyAddedAttendees?.length <= 5"
                    right
                    split
                    variant="outline"
                    @click="rejectAttendees(newlyAddedAttendees)"
                >
                    <template #button-content>
                        <bm-label-icon icon="cross" @click="rejectAttendees(newlyAddedAttendees)">
                            {{
                                $tc("mail.viewer.invitation.counter.added_attendees.refuse", 1, {
                                    count: newlyAddedAttendees?.length
                                })
                            }}
                        </bm-label-icon>
                    </template>

                    <bm-dropdown-item
                        v-for="(attendee, index) in newlyAddedAttendees"
                        :key="index"
                        role="menuitem"
                        @click="rejectAttendees(attendee)"
                        >{{
                            $tc("mail.viewer.invitation.counter.added_attendees.refuse", 0, { attendee: attendee.dn })
                        }}</bm-dropdown-item
                    >
                </bm-dropdown>
                <bm-toggleable-button v-else icon="cross" @click="rejectAttendees(newlyAddedAttendees)">
                    {{
                        $tc(
                            "mail.viewer.invitation.counter.added_attendees.refuse",
                            newlyAddedAttendees?.length === 1 ? 0 : 3,
                            {
                                attendee: newlyAddedAttendees?.[0]?.dn
                            }
                        )
                    }}
                </bm-toggleable-button>
            </template>
        </event-header>

        <event-detail :event="event" :message="message" />

        <div class="event-footer">
            <event-footer-section
                :label="
                    $tc('mail.viewer.invitation.counter.added_attendees', newlyAddedAttendees?.length, {
                        count: newlyAddedAttendees?.length
                    })
                "
            >
                <div
                    v-for="(attendee, index) in newlyAddedAttendees"
                    :key="index"
                    class="event-footer-entry"
                    role="listitem"
                >
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
        </div>
    </div>
</template>
