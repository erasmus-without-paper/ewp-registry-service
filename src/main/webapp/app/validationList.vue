<template>
    <div class="validation-list">
        <validation-entry v-for="entry in entries" :entry="entry"></validation-entry>
    </div>
</template>

<script>
    import validationEntry from "./validationEntry";

    class ValidationRequest {
        constructor(url, name, endpoint, version, security, parameters) {
            this.url = url;
            this.name = name;
            this.endpoint = endpoint;
            this.security = security;
            this.version = version;
            this.parameters = parameters;
            this.text = "In progress...";
            this.status = "IN_PROGRESS";
        }
    }

    export default {
        name: "validationList",
        components: {
            validationEntry
        },
        data: function() {
            return {
                entries: []
            }
        },
        methods: {
            addEntry: function(url, name, endpoint, version, security, parameters) {
                this.entries.push(new ValidationRequest(url, name, endpoint, version, security, parameters));
                this.entries = this.entries.slice(-5);
            }
        }
    }
</script>

<style scoped>

</style>