<template>
    <div class="validation-entry">
        <span class="validation-entry-status">
            <span v-if='entry.status === "IN_PROGRESS"'>In progress...</span>
            <span v-else-if='entry.status === "ERROR"'>Error {{ entry.response.status }}</span>
            <span v-else-if='entry.status === "DONE"'> <a href="#" @click.prevent="openResult">Show results</a> </span>
        </span>
        <span class="validation-entry-template-security"> {{ entry.security }} </span>
        <code v-for="param in entry.parameters">
            <br /> <span class="validation-entry-param-name">{{ param.name }}</span> ← {{ param.value }}
        </code>
    </div>
</template>

<script>
    export default {
        name: "validationEntry",
        props: {
            entry: Object
        },
        methods: {
            openResult: function() {
                const newWindow = window.open();
                newWindow.document.write(this.entry.response);
                newWindow.document.close();
            }
        },
    }
</script>

<style scoped>

</style>