<template>
    <event-viewer v-if="message.hasICS && currentEvent" :message="message" />
    <parts-viewer v-else :message="message" :collapse-quoted="collapseQuoted" />
</template>

<script>
import { mapState } from "vuex";
import EventViewer from "./EventViewer";
import PartsViewer from "./PartsViewer/PartsViewer";

export default {
    name: "BodyViewer",
    components: { EventViewer, PartsViewer },
    props: {
        message: {
            type: Object,
            required: true
        },
        collapseQuoted: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        ...mapState("mail", { currentEvent: state => state.consultPanel.currentEvent })
    }
};
</script>
