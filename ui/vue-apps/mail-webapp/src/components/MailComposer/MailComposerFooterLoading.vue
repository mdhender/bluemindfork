<template>
    <div class="mail-composer-footer-loading d-flex p-2 border-top justify-content-between align-items-center">
        <div>
            <bm-button type="submit" variant="primary" disabled>
                {{ $t("common.send") }}
            </bm-button>
            <bm-button variant="simple-dark" class="ml-2" disabled>
                {{ $t("common.delete") }}
            </bm-button>
        </div>
        <div class="align-items-center toolbar">
            <bm-button variant="simple-dark" disabled>
                <bm-icon icon="text-format" size="lg" />
            </bm-button>
            <bm-button variant="simple-dark" disabled>
                <bm-icon icon="paper-clip" size="lg" />
            </bm-button>
        </div>
    </div>
</template>

<script>
import { BmButton, BmIcon } from "@bluemind/styleguide";

export default {
    name: "MailComposerFooterLoading",
    components: { BmButton, BmIcon }
};
</script>
