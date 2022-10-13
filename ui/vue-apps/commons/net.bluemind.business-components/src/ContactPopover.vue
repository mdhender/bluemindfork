<template>
    <bm-popover triggers="click blur" :target="target" placement="bottom" no-fade>
        <resolved-contact :criteria="criteria">
            <template v-slot:default="{ resolvedContact }">
                <contact-card :contact="resolvedContact" />
            </template>
        </resolved-contact>
    </bm-popover>
</template>

<script>
import { BmPopover } from "@bluemind/styleguide";
import ContactCard from "./ContactCard";
import ResolvedContact from "./ResolvedContact";

export default {
    name: "ContactPopover",
    components: { BmPopover, ContactCard, ResolvedContact },
    props: {
        criteria: {
            type: Object,
            required: true
        },
        target: {
            type: String,
            required: true
        }
    }
};
</script>
