<template>
    <div class="mail-composer-footer-loading d-flex p-2 border-top justify-content-between align-items-center">
        <div>
            <bm-button type="submit" variant="fill-accent" icon="send" disabled>
                {{ $t("common.send") }}
            </bm-button>
            <bm-button variant="text" class="ml-6" icon="trash" disabled>
                {{ $t("common.delete") }}
            </bm-button>
        </div>
        <div class="align-items-center toolbar">
            <bm-icon-button variant="compact" size="lg" icon="letter" disabled />
            <bm-icon-button variant="compact" size="lg" icon="paperclip" disabled />
        </div>
    </div>
</template>

<script>
import { BmButton, BmIconButton } from "@bluemind/ui-components";

export default {
    name: "MailComposerFooterLoading",
    components: { BmButton, BmIconButton }
};
</script>
