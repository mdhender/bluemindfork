<template>
    <div class="mail-toolbar-responsive-button">
        <bm-icon-button
            class="d-inline-flex d-lg-none"
            variant="compact-on-fill-primary"
            size="lg"
            v-bind="[$attrs, $props]"
            :title="title ? title : label"
            v-on="$listeners"
        />
        <bm-captioned-icon-button
            class="d-none d-lg-inline-flex"
            v-bind="[$attrs, $props]"
            :caption="label"
            v-on="$listeners"
        />
    </div>
</template>

<script>
import { BmIconButton, BmCaptionedIconButton } from "@bluemind/styleguide";

export default {
    name: "MailToolbarResponsiveButton",
    components: { BmIconButton, BmCaptionedIconButton },
    props: {
        icon: {
            type: String,
            required: true
        },
        label: {
            type: String,
            required: true
        },
        title: {
            type: String,
            default: null
        }
    }
};
</script>
