<template>
    <bm-toolbar-icon-button
        v-if="compact"
        :class="className"
        variant="compact"
        size="lg"
        :icon="icon"
        v-bind="[$attrs, $props]"
        :title="title ? title : label"
        v-on="$listeners"
    />
    <div v-else :class="className">
        <bm-toolbar-icon-button
            class="mobile-only"
            variant="compact-on-fill-primary"
            size="lg"
            v-bind="[$attrs, $props]"
            :title="title ? title : label"
            v-on="$listeners"
        />
        <bm-toolbar-captioned-icon-button
            class="desktop-only"
            v-bind="[$attrs, $props]"
            :caption="label"
            v-on="$listeners"
        />
    </div>
</template>

<script>
import { BmToolbarIconButton, BmToolbarCaptionedIconButton } from "@bluemind/ui-components";

export default {
    name: "MailToolbarResponsiveButton",
    components: { BmToolbarIconButton, BmToolbarCaptionedIconButton },
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
    },
    data() {
        return {
            className: "mail-toolbar-responsive-button mail-toolbar-item"
        };
    }
};
</script>
