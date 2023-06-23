<template>
    <div class="selected-contacts selected bg-surface">
        <contact-input :contacts.sync="selectedContacts" readonly variant="inline" class="ml-6 flex-fill">
            <span class="selected-label">{{ $t("common.to") }}</span>
        </contact-input>
    </div>
</template>

<script>
import { ContactInput } from "@bluemind/business-components";

export default {
    name: "SelectedContacts",
    components: { ContactInput },
    props: {
        contacts: { type: Array, default: undefined }
    },
    computed: {
        selectedContacts: {
            get() {
                return this.contacts;
            },
            set(value) {
                this.$emit("update:contacts", value);
            }
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/mixins/responsiveness";
@import "@bluemind/ui-components/src/css/type";
@import "@bluemind/ui-components/src/css/variables";
@import "@bluemind/ui-components/src/css/mixins";

.selected-contacts {
    display: flex;
    align-items: flex-start;

    min-height: $avatar-height-sm + base-px-to-rem(20);
    .contact-input {
        .selected-label {
            color: $neutral-fg-hi1;
            @extend %bold;
            margin-right: $sp-5;
            @include from-lg {
                margin-right: $sp-6;
            }
            height: 100%;
            vertical-align: top;
            padding-top: base-px-to-rem(12);
        }
        .contacts {
            @include scroller-y;
            max-height: base-px-to-rem(124);
            padding-top: base-px-to-rem(4);
        }
    }
}
</style>
