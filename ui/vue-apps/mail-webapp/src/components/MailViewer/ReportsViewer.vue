<template>
    <div class="reports-viewer">
        <div v-for="report in message.reports" :key="report.address" class="border border-primary p-4">
            <component :is="componentName(report)" :message="message" :file="report" />
        </div>
    </div>
</template>

<script>
import capitalize from "lodash.capitalize";
import MessageDispositionNotificationFileViewer from "./FilesViewer/MessageDispositionNotificationFileViewer";

export default {
    name: "ReportsViewer",
    components: { MessageDispositionNotificationFileViewer },
    props: { message: { type: Object, required: true } },
    methods: {
        componentName(report) {
            const capitalized = report.mime.split(/[/-]/).map(capitalize).join("");
            return `${capitalized}FileViewer`;
        }
    }
};
</script>
