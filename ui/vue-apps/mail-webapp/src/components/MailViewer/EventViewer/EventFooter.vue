<script setup>
import { computed, ref } from "vue";
import store from "@bluemind/store";
import { darkifyHtml, darkifyingBaseLvalue, BmButton, BmLabelIcon } from "@bluemind/ui-components";
import EventFooterSection from "./EventFooterSection.vue";

const props = defineProps({ event: { type: Object, required: true } });

const getAttendeesByCutype = cutype =>
    props.event.attendees
        ?.filter(attendee => attendee.cutype === cutype)
        .map(({ name, mail }) => ({ name, text: mail })) ?? [];
const attendees = computed(() => getAttendeesByCutype("Individual"));
const resources = computed(() => getAttendeesByCutype("Resource"));

const showFooter = computed(() =>
    Boolean(attendees.value.length || resources.value.length || props.event?.sanitizedDescription)
);

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
</script>

<template>
    <div v-if="showFooter" class="event-footer">
        <div v-if="event.conference">
            <bm-button variant="fill-accent" class="event-footer-conference" @click="openConference">
                <bm-label-icon icon="video"> {{ $t("mail.viewer.invitation.conference") }} </bm-label-icon>
            </bm-button>
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

.event-footer {
    padding: 0 $sp-5 0 $sp-2;
    display: flex;
    flex-direction: column;
    gap: $sp-4;

    .event-footer-conference {
        margin: $sp-5 0 $sp-5 $sp-6 + $sp-3;
    }

    .event-footer-description {
        word-wrap: break-word;
    }
}
</style>
