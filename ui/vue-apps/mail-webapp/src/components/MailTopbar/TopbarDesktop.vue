<template>
    <div class="topbar-desktop" :class="{ 'active-search': activeSearch }">
        <div class="new">
            <new-message :template="activeFolder === MY_TEMPLATES.key" />
        </div>
        <div class="search">
            <mail-search-box @active="activeSearch = $event" />
        </div>
        <div class="toolbar">
            <mail-toolbar :compact="activeSearch" />
        </div>
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

    .new {
        padding: 0 $sp-5;
        display: flex;
        justify-content: center;
        width: 16.7%;
    }

    .toolbar {
        flex: 0 1;
        order: 2;
        @include until-lg {
            display: none;
        }
    }
    & > .search {
        width: 25%;
    }

    &.active-search {
        & > .search {
            flex-grow: 2;
            max-width: 80%;
            .mail-search-box-context {
                width: calc(16.67vw - 3.5rem);
            }
        }
        & > .new {
            height: base-px-to-rem(30);
            width: 3.5rem !important;
            .slot-wrapper {
                display: none;
            }
        }
    }
}
</style>
