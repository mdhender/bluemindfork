<template>
    <div class="topbar-desktop" :class="{ 'active-search': activeSearch }">
        <div class="new">
            <new-message :template="activeFolder === MY_TEMPLATES.key" />
        </div>
        <mail-search-box class="search" @active="activeSearch = $event" />
        <mail-toolbar :compact="activeSearch" />
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { MY_TEMPLATES } from "~/getters";
import NewMessage from "../NewMessage";
import MailSearchBox from "../MailSearch/MailSearchBox";
import MailToolbar from "../MailToolbar/MailToolbar";

export default {
    components: {
        MailSearchBox,
        MailToolbar,
        NewMessage
    },
    data() {
        return {
            activeSearch: false
        };
    },
    computed: {
        ...mapState("mail", {
            activeFolder: "activeFolder"
        }),
        ...mapGetters("mail", {
            MY_TEMPLATES
        })
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/typography";
.topbar-desktop {
    background-color: $surface-hi1;
    height: base-px-to-rem(48);

    display: flex;
    flex: 1 1 auto;
    align-items: center;
    max-width: 100%;

    .new {
        padding: 0 $sp-5;
        display: flex;
        justify-content: center;
        width: 16.7%;
    }

    &:not(.active-search) > .search {
        flex: 0 0 base-px-to-rem(500);
    }

    &.active-search {
        & > .new {
            height: base-px-to-rem(30);
            width: 3.5rem !important;
            .slot-wrapper {
                display: none;
            }
        }
        & > .search {
            flex: 0 1 base-px-to-rem(1600);
            .mail-search-box-context {
                width: calc(16.67vw - 3.5rem);
            }
        }
    }

    .mail-toolbar {
        order: 2;
        flex: 1 1 base-px-to-rem(400);
        @include until-lg {
            display: none;
        }
        min-width: 0;
    }
}
</style>
