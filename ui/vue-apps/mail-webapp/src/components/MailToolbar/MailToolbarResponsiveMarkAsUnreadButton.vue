<template>
    <div class="mail-toolbar-responsive-button mail-toolbar-item mail-toolbar-responsive-mark-as-unread-button">
        <mark-as-unread-icon-button
            class="d-inline-flex d-lg-none"
            variant="compact-on-fill-primary"
            size="lg"
            v-bind="[$attrs, $props]"
            v-on="$listeners"
        />
        <mark-as-unread-captioned-icon-button
            class="d-none d-lg-inline-flex"
            v-bind="[$attrs, $props]"
            v-on="$listeners"
        />
    </div>
</template>

<script>
import MarkAsUnreadIconButton from "../MarkAsUnreadIconButton";
import MarkAsUnreadCaptionedIconButton from "../MarkAsUnreadCaptionedIconButton";

export default {
    name: "MailToolbarResponsiveMarkAsUnreadButton",
    components: { MarkAsUnreadIconButton, MarkAsUnreadCaptionedIconButton }
};
</script>
