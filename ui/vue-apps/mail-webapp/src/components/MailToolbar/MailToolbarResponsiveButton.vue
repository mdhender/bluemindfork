<template>
    <div class="mail-toolbar-responsive-button mail-toolbar-item">
        <bm-icon-button
            v-if="compact"
            variant="compact"
            size="lg"
            v-bind="[$attrs, $props]"
            :title="title ? title : label"
            v-on="$listeners"
        />
        <template v-else>
            <bm-icon-button
                class="mobile-only"
                variant="compact-on-fill-primary"
                size="lg"
                v-bind="[$attrs, $props]"
                :title="title ? title : label"
                v-on="$listeners"
            />
            <bm-captioned-icon-button
                class="desktop-only"
                v-bind="[$attrs, $props]"
                :caption="label"
                v-on="$listeners"
            />
        </template>
    </div>
</template>

<script>
import { BmIconButton, BmCaptionedIconButton } from "@bluemind/ui-components";

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
        },
        compact: {
            type: Boolean,
            default: false
        }
    }
};
</script>
