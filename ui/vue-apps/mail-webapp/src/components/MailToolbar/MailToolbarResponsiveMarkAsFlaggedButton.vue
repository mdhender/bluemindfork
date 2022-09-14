<template>
    <div class="mail-toolbar-responsive-button mail-toolbar-item mail-toolbar-responsive-mark-as-flagged-button">
        <mark-as-flagged-icon-button
            class="d-inline-flex d-lg-none"
            variant="compact-on-fill-primary"
            size="lg"
            v-bind="[$attrs, $props]"
            v-on="$listeners"
        />
        <mark-as-flagged-captioned-icon-button
            class="d-none d-lg-inline-flex"
            v-bind="[$attrs, $props]"
            v-on="$listeners"
        />
    </div>
</template>

<script>
import MarkAsFlaggedIconButton from "../MarkAsFlaggedIconButton";
import MarkAsFlaggedCaptionedIconButton from "../MarkAsFlaggedCaptionedIconButton";

export default {
    name: "MailToolbarResponsiveMarkAsFlaggedButton",
    components: { MarkAsFlaggedIconButton, MarkAsFlaggedCaptionedIconButton }
};
</script>
