<script setup>
import { computed } from "vue";
import store from "@bluemind/store";
import { BmToggleableButton, BmDropdown, BmDropdownItem, BmLabelIcon } from "@bluemind/ui-components";
import { REJECT_ATTENDEES } from "~/actions";
import EventHeader from "./base/EventHeader";
import EventDetail from "./base/EventDetail";
import AttendeeList from "./AttendeeList";
import { EmailExtractor } from "@bluemind/email";
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
const rejectedAttendees = computed(() => {
    const attendeesMail = props.event.attendees?.map(a => a.mail);
    return attendeeHeader.value.values?.[0]
        .split(",")
        .map(str => ({ address: EmailExtractor.extractEmail(str), dn: EmailExtractor.extractDN(str) }))
        .filter(anAttendee => !attendeesMail.includes(anAttendee.address));
});

function rejectAttendees(attendees) {
    store.dispatch(`mail/${REJECT_ATTENDEES}`, { rejectedAttendees: attendees });
}
</script>

<template>
    <div v-if="attendeeHeader" class="event-countered">
        <event-header>
            <span class="bold"> {{ $t("mail.viewer.invitation.counter.attendees") }}</span>
            <template v-if="newlyAddedAttendees?.length" #actions>
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
                        @click="rejectAttendees([attendee])"
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
            <attendee-list
                :label="$tc('mail.viewer.invitation.counter.added_attendees', newlyAddedAttendees?.length)"
                :attendees="newlyAddedAttendees"
            />
            <attendee-list
                :label="$tc('mail.viewer.invitation.counter.rejected_attendees', rejectedAttendees?.length)"
                :attendees="rejectedAttendees"
            />
        </div>
    </div>
</template>
