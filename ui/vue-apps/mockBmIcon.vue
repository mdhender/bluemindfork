<template>
    <svg :data-test-id="icon"></svg>
</template>

<script setup>
defineProps({
    icon: { type: String, default: undefined }
});
</script>
