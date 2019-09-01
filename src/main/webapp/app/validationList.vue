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
            this.sendRequest();
        }

        sendRequest() {
            $.ajax({
                url: "/validateApi",
                data: JSON.stringify({
                    url: this.url,
                    name: this.name,
                    endpoint: this.endpoint,
                    version: this.version,
                    security: this.security,
                    parameters: this.parameters
                }),
                type: "POST",
                contentType: 'application/json',
                dataType: "html"
            }).done((result) => this.onSuccess(result))
                .fail((result) => this.onError(result));
        }

        onError(result) {
            this.status = "ERROR";
            this.response = result;
        }

        onSuccess(result) {
            this.status = "DONE";
            this.response = result;
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
                this.entries.unshift(new ValidationRequest(url, name, endpoint, version, security, parameters));
                this.entries = this.entries.slice(0, 5);
            }
        }
    }
</script>

<style scoped>

</style>