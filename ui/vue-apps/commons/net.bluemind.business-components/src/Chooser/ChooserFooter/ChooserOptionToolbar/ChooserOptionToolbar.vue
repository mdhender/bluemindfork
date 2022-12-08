<template>
    <bm-button-group class="chooser-option-toolbar">
        <div class="pr-5 d-flex align-items-center">{{ $t("chooser.insert_as") }}</div>
        <insert-as-link-button :disabled="disabledAsLinks" @click.native="$emit('insert-as-link')" />
        <insert-as-attachment-button :disabled="disabledAsAttachments" @click.native="$emit('insert-as-attachment')" />
    </bm-button-group>
</template>

<script>
import { BmButtonGroup } from "@bluemind/ui-components";
import InsertAsAttachmentButton from "./InsertAsAttachmentButton";
import InsertAsLinkButton from "./InsertAsLinkButton";
export default {
    name: "ChooserOptionToolbar",
    components: { BmButtonGroup, InsertAsAttachmentButton, InsertAsLinkButton },
    props: {
        disabledAsAttachments: {
            type: Boolean,
            default: false
        },
        disabledAsLinks: {
            type: Boolean,
            default: false
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";
@import "~@bluemind/ui-components/src/css/mixins/_responsiveness";

.chooser-option-toolbar {
    .btn-outline {
        &.insert-as-attachment-button {
            border-left: none !important;
        }
    }

    @include until-lg {
        margin-bottom: $sp-6;
        &.btn-group {
            display: block;
        }
    }
}
</style>
