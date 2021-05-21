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
#content-menu:empty,
#content-right:empty,
#content-body:empty,
#full:empty,
#navigation:empty,
#sub-navigation:empty,
#main:empty {
    display: none;
}

main {
    color: #666;

    *,
    *::before,
    *::after {
        box-sizing: initial;
    }

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
}

.modal-dialog {
    position: absolute !important;
    pointer-events: auto !important;
    width: 475px !important;
    margin: 0 !important;
}
</style>
