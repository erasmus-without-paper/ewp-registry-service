<template>
    <div class="api-entry">
        <div class="api-info">
            <span class="api-name">{{name}}{{endpoint ? ' - ' + endpoint : ''}}</span> {{version}} <code>{{url}}</code>
        </div>
        <div class="api-entry-content">
            <div class="securities">
                <div class="section-name">Security method
                    <global-help-popup-button popup-id="security-method-popup"></global-help-popup-button>
                </div>
                <security-radio-button v-for="sec in security" :name="sec" :group="url" @selected='onSecuritySelected'>
                </security-radio-button>
            </div>

            <parameters ref="inputParameters" :parameters="parameters"></parameters>

            <div class="validation">
                <button class="validation-button" @click='validate' :disabled='!isSecuritySelected'> Validate </button>
                <validation-list ref='validationList'></validation-list>
            </div>
        </div>
        <div v-if="hasNotices" class="api-entry-notice-list">
            <p>Detected problems with securities:</p>
            <ul>
                <li v-for="securityNotice in securityNotices">{{ securityNotice }}</li>
            </ul>
        </div>
    </div>
</template>

<script>
    import parameters from './parameters'
    import globalHelpPopupButton from './globalHelpPopupButton'
    import securityRadioButton from './securityRadioButton'
    import validationList from './validationList'

    export default {
        name: "apiEntry",
        components: {
            parameters,
            globalHelpPopupButton,
            securityRadioButton,
            validationList
        },
        props: {
            name: String,
            version: String,
            endpoint: String,
            security: Array,
            url: String,
            parameters: Array,
            securityNotices: Array
        },
        data: function() {
            return {
                selectedSecurity: ''
            };
        },
        computed: {
            isSecuritySelected: function() {
                return this.selectedSecurity !== "";
            },
            hasNotices: function () {
                return Array.isArray(this.securityNotices) && this.securityNotices.length > 0;
            }
        },
        methods: {
            onSecuritySelected: function(value) {
                this.selectedSecurity = value;
            },
            validate: function() {
                this.$refs.validationList.addEntry(
                    this.url, this.name, this.endpoint, this.version, this.selectedSecurity, this.gatherParameters());
            },
            gatherParameters: function() {
                return this.$refs.inputParameters.gatherParameters();
            }
        },
    }
</script>

<style scoped>

</style>