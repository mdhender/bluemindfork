<template>
    <event-viewer v-if="message.hasICS && currentEvent" :message="message" class="body-viewer" />
    <parts-viewer v-else :message="message" class="body-viewer" />
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
        }
    },
    computed: {
        ...mapState("mail", { currentEvent: state => state.consultPanel.currentEvent })
    }
};
</script>
