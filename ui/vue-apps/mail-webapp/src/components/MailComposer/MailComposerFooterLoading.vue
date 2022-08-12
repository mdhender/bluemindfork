<template>
    <div class="mail-composer-footer-loading d-flex p-2 border-top justify-content-between align-items-center">
        <div>
            <bm-button type="submit" variant="contained-accent" icon="send" disabled>
                {{ $t("common.send") }}
            </bm-button>
            <bm-button variant="text" class="ml-6" icon="trash" disabled>
                {{ $t("common.delete") }}
            </bm-button>
        </div>
        <div class="align-items-center toolbar">
            <bm-icon-button variant="compact" size="lg" icon="text-format" disabled />
            <bm-icon-button variant="compact" size="lg" icon="paper-clip" disabled />
        </div>
    </div>
</template>

<script>
import { BmButton, BmIconButton } from "@bluemind/styleguide";

export default {
    name: "MailComposerFooterLoading",
    components: { BmButton, BmIconButton }
};
</script>
