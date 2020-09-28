<template>
    <div class="d-flex flex-column bg-white mail-composer-panel">
        <slot name="header">
            <mail-composer-panel-header :title="title" :closeable="closeable" @remove="$emit('remove')" />
        </slot>
        <div class="flex-grow-1 d-flex flex-column">
            <slot name="body" />
        </div>
        <div class="p-2 border-top mail-composer-panel-footer">
            <slot name="footer" />
        </div>
    </div>
</template>

<script>
import MailComposerPanelHeader from "./MailComposerPanelHeader";

export default {
    name: "MailComposerPanel",
    components: {
        MailComposerPanelHeader
    },
    props: {
        title: {
            type: String,
            required: false,
            default: ""
        },
        closeable: {
            type: Boolean,
            required: false,
            default: true
        }
    }
};
</script>
