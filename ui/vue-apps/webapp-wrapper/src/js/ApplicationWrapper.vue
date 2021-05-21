<template functional>
    <main class="bg-white flex-fill">
        <div id="header" data-banner="false"></div>
        <div id="content-menu"></div>
        <div id="content-right"></div>
        <div id="content-body"></div>
        <div id="full"></div>
        <div id="navigation"></div>
        <div id="sub-navigation"></div>
        <div id="main"></div>
    </main>
</template>
<script>
export default {
    name: "ApplicationWrapper"
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";
/* Body selector is used to overwrite default selector */
body {
    #header:empty,
    #content-menu:empty,
    #content-right:empty,
    #content-body:empty,
    #full:empty,
    #navigation:empty,
    #sub-navigation:empty,
    #main:empty {
        display: none;
    }

    /*Restore box sizing for banner */
    .bm-banner,
    .preferences {
        box-sizing: border-box;
        line-height: $line-height-base;
        .bm-circular-progress.primary {
            background-image: none;
            background-color: transparent;
        }
    }

    /* Closure overwrite */
    main {
        color: #666;
        h2 {
            margin-block-start: 0.83em;
            margin-block-end: 0.83em;
        }
        .goog-button-base {
            line-height: inherit;
        }
        .bm-ui-form-inline label {
            width: auto;
        }
        div.form-notification {
            height: auto;
        }
        *,
        *::before,
        *::after {
            box-sizing: revert;
        }
    }
    /* GWT overwrite */

    .root-layout {
        color: #666;
        top: 43px !important;
        line-height: 1;
        *,
        *::before,
        *::after {
            box-sizing: revert;
        }
    }

    .modal-dialog {
        position: absolute !important;
        pointer-events: auto !important;
        width: 475px !important;
        margin: 0 !important;
    }

    .banner {
        display: none;
    }
}
</style>
