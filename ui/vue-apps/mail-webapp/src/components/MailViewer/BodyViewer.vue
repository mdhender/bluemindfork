<template>
    <div class="body-viewer">
        <event-viewer v-if="message.hasICS && currentEvent" :current-event="currentEvent" :message="message" />
        <parts-viewer v-else :message="message" />
    </div>
</template>
<script>
import { mapState } from "vuex";
import EventViewer from "./EventViewer.vue";
import PartsViewer from "./PartsViewer/PartsViewer";

export default {
    name: "BodyViewer",
    components: {
        EventViewer,
        PartsViewer
    },
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
