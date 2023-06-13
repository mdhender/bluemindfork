<template>
    <div class="empty-templates-folder">
        <div class="empty-folder-text">
            {{ $t("mail.folder") }}
            <mail-folder-icon :folder="MY_TEMPLATES" class="font-weight-bold" />
            {{ $t("mail.empty") }}
        </div>
        <bm-illustration value="empty-folder" size="md" over-background />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import { BmIllustration } from "@bluemind/ui-components";

import MailFolderIcon from "../MailFolderIcon";
import { MY_TEMPLATES } from "~/getters";

export default {
    name: "EmptyTemplatesFolder",
    components: {
        BmIllustration,
        MailFolderIcon
    },
    computed: {
        ...mapGetters("mail", { MY_TEMPLATES })
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables.scss";

.empty-templates-folder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    .empty-folder-text {
        display: flex;
        flex-wrap: wrap;
        gap: 0 base-px-to-rem(5);
        padding: 0 $sp-5;
    }

    .bm-illustration {
        max-width: 100%;
    }
}
</style>
