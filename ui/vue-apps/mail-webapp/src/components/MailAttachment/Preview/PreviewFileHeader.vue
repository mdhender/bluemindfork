<template>
    <bm-navbar-title class="preview-file-header text-truncate">
        <bm-file-icon :file="file" />
        <div :title="file.name" class="text-truncate">{{ file.name }}</div>
    </bm-navbar-title>
</template>

<script>
import { BmFileIcon, BmNavbarTitle } from "@bluemind/ui-components";

export default {
    name: "PreviewFileHeader",
    components: { BmFileIcon, BmNavbarTitle },
    props: {
        file: {
            type: Object,
            required: true
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/typography";
@import "~@bluemind/ui-components/src/css/utils/variables";

.preview-file-header {
    display: flex;
    align-items: center;

    @include until-lg {
        @include bold-tight;
    }

    @include from-lg {
        background-color: $neutral-bg;
        &.bm-navbar-title {
            padding-left: 0 !important;
        }
    }

    .bm-file-icon {
        margin-right: $sp-4;
        @include from-lg {
            margin-right: $sp-5;
        }

        @include until-lg {
            .bm-icon {
                $size: map-get($icon-sizes, "lg");
                width: $size;
                height: $size;
            }
        }
    }
}
</style>
