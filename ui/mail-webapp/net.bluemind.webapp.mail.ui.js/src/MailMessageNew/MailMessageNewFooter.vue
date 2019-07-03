<template>
    <div>
        <bm-button type="submit" variant="primary" @click.prevent="$emit('send')">
            {{ $t("common.send") }}
        </bm-button>
        <bm-button variant="outline-primary" class="ml-1" @click.prevent="$emit('save')">
            {{ $t("common.save") }}
        </bm-button>
        <bm-button variant="link" class="ml-3" @click.prevent="$emit('close')">
            {{ $t("common.delete") }}
        </bm-button>
    </div>
</template>

<script>
import BmButton from "@bluemind/styleguide/components/buttons/BmButton";

export default {
    name: "MailMessageNewFooter",
    components: {
        BmButton
    }
};
</script>