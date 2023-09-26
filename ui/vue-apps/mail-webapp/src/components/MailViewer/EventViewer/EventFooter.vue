<script setup>
import { computed, ref } from "vue";
import i18n from "@bluemind/i18n";
import store from "@bluemind/store";
import { darkifyHtml, darkifyingBaseLvalue, BmDropdown, BmDropdownItem, BmLabelIcon } from "@bluemind/ui-components";
import EventFooterSection from "./EventFooterSection.vue";

const props = defineProps({ event: { type: Object, required: true } });

const organizer = {
    name: props.event.organizer.name,
    text: props.event.organizer.mail,
    detail: i18n.t("common.organizer")
};
const getAttendeesByCutype = cutype =>
    props.event.attendees
        ?.filter(attendee => attendee.cutype === cutype)
        .map(({ name, mail }) => ({ name, text: mail })) ?? [];
const individuals = computed(() => getAttendeesByCutype("Individual"));
const resources = computed(() => getAttendeesByCutype("Resource"));
const attendees = computed(() => [organizer, ...individuals.value]);

const description = computed(() => {
    if (!props.event.sanitizedDescription) {
        return undefined;
    }

    if (store.getters["settings/IS_COMPUTED_THEME_DARK"]) {
        return darkify(props.event.sanitizedDescription);
    }
    return props.event.sanitizedDescription;
});

function darkify(text) {
    const customProperties = new Map();
    const htmlDoc = new DOMParser().parseFromString(props.event.sanitizedDescription, "text/html");
    darkifyHtml(htmlDoc, darkifyingBaseLvalue(), customProperties);

    let cssStr = "\n.event-footer-description {\n";
    for (const [key, value] of customProperties) {
        cssStr += `    ${key}: ${value};\n`;
    }
    cssStr += "}\n";
    const styleElement = document.createElement("style");
    styleElement.innerHTML = cssStr;
    const htmlBody = htmlDoc.documentElement.querySelector("body");
    htmlBody.appendChild(styleElement);

    return htmlBody.innerHTML;
}

function openConference() {
    window.open(props.event.conference);
}
function copyLink() {
    navigator.clipboard.writeText(props.event.conference);
}
</script>

<template>
    <div class="event-footer">
        <div>
            <bm-dropdown
                v-if="event.conference"
                variant="fill-accent"
                class="event-footer-conference"
                text=""
                split
                right
                @click="openConference"
            >
                <template #button-content>
                    <bm-label-icon icon="video">
                        {{ $t("mail.viewer.invitation.conference") }}
                    </bm-label-icon>
                </template>
                <bm-dropdown-item icon="copy" @click="copyLink">{{ $t("common.copy.link") }}</bm-dropdown-item>
            </bm-dropdown>
        </div>

        <event-footer-section
            v-if="attendees?.length"
            :label="$tc('mail.viewer.invitation.attendee', attendees.length, { count: attendees.length })"
            :entries="attendees"
        />
        <event-footer-section
            v-if="resources.length"
            :label="$tc('mail.viewer.invitation.resource', resources.length, { count: resources.length })"
            :entries="resources"
        />
        <event-footer-section v-if="description" :label="$t('mail.viewer.invitation.description')">
            <!-- eslint-disable-next-line vue/no-v-html -->
            <div class="event-footer-description" v-html="description" />
        </event-footer-section>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";

.event-footer {
    padding: 0 $sp-5 0 $sp-2;
    display: flex;
    flex-direction: column;
    gap: $sp-4;

    .event-footer-conference {
        margin: $sp-5 0 $sp-5 $sp-6 + $sp-3;
        @include until-lg {
            margin-left: $sp-4;
        }
    }

    .event-footer-description {
        word-wrap: break-word;
    }
}
</style>
