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
        created() {
            $.ajax({
                url: "/validateApi",
                data: JSON.stringify({
                    url: this.entry.url,
                    name: this.entry.name,
                    endpoint: this.entry.endpoint,
                    version: this.entry.version,
                    security: this.entry.security,
                    parameters: this.entry.parameters
                }),
                type: "POST",
                contentType: 'application/json',
                dataType: "html",
                success: result => {
                    this.entry.status = "DONE";
                    this.entry.response = result;
                },
                fail: result => {
                    this.entry.status = "ERROR";
                    this.entry.response = result;
                }
            })
      }
    }
</script>

<style scoped>

</style>