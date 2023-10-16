<script setup>
import { computed } from "vue";
import i18n from "@bluemind/i18n";
import store from "@bluemind/store";
import { html2text } from "@bluemind/html-utils";
import { BmDropdown, BmDropdownItem, BmLabelIcon } from "@bluemind/ui-components";
import { Contact } from "@bluemind/business-components";
import EventFooterSection from "./EventFooterSection.vue";
import MailContactCardSlots from "../../MailContactCardSlots";

const props = defineProps({ event: { type: Object, required: true } });

const organizer = computed(() => props.event.organizer || {});
const attendees = computed(() =>
    [organizer.value, ...getAttendeesByCutype(props.event.attendees, "Individual")].map(attendee => ({
        dn: attendee?.name,
        address: attendee?.mail
    }))
);

const resources = computed(() =>
    getAttendeesByCutype(props.event.attendees, "Resource").map(({ name, mail }) => ({
        name,
        text: mail
    }))
);

function getAttendeesByCutype(attendees = [], cutype) {
    return attendees.filter(attendee => attendee.cutype === cutype);
}

const description = computed(() => {
    if (!props.event.sanitizedDescription) {
        return undefined;
    }
    return html2text(props.event.sanitizedDescription);
});

function openConference() {
    window.open(props.event.conference);
}
function copyLink() {
    navigator.clipboard.writeText(props.event.conference);
}
</script>

<template>
    <div class="event-footer">
        <div v-if="event.conference">
            <bm-dropdown variant="fill-accent" class="event-footer-conference" split right @click="openConference">
                <template #button-content>
                    <bm-label-icon icon="video">
                        {{ $t("mail.viewer.invitation.conference") }}
                    </bm-label-icon>
                </template>
                <bm-dropdown-item icon="copy" @click="copyLink">{{ $t("common.copy.link") }}</bm-dropdown-item>
            </bm-dropdown>
        </div>

        <event-footer-section
            :label="$tc('mail.viewer.invitation.attendee', attendees?.length, { count: attendees?.length })"
        >
            <div v-for="(attendee, index) in attendees" :key="index" class="event-footer-entry">
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
                <div v-if="attendee.address === organizer.mail" class="organizer caption-bold">
                    &nbsp;({{ $t("common.organizer") }})
                </div>
            </div>
        </event-footer-section>
        <event-footer-section
            v-if="resources.length"
            :label="$tc('mail.viewer.invitation.resource', resources.length, { count: resources.length })"
            :entries="resources"
        />
        <event-footer-section v-if="description" :label="$t('mail.viewer.invitation.description')">
            <div class="event-footer-description" v-text="description" />
        </event-footer-section>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";

.event-footer {
    padding-top: $sp-5;
    @include from-lg {
        padding-top: $sp-4;
        padding-left: $sp-2;
    }
    padding-right: $sp-5;
    display: flex;
    flex-direction: column;
    gap: $sp-5;
    @include from-lg {
        gap: $sp-4;
    }

    .event-footer-conference {
        margin: $sp-5 0 $sp-5 $sp-6 + $sp-3;
        @include until-lg {
            margin-left: $sp-4;
        }
    }

    .event-footer-description {
        word-wrap: break-word;
        white-space: break-spaces;
    }

    .event-footer-entry {
        .organizer {
            color: $neutral-fg-lo1;
        }
    }
}
</style>
