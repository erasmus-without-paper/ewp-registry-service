<template>
    <div class="parameters">
        <div class="section-name">Parameters
            <global-help-popup-button popup-id="parameters-popup"> </global-help-popup-button>
        </div>
        <table v-if="parameters && parameters.length > 0">
            <parameter-input v-for="parameter in parameters"
                             ref="parameter_input"
                             :name="parameter.name"
                             :dependencies="parameter.dependencies"
                             :blockers="parameter.blockers"
                             :description="parameter.description"
                             @emptyChanged='emptyChanged'
            ></parameter-input>
        </table>
        <div v-else>
            No parameters available.
        </div>
    </div>
</template>

<script>
    import parameterInput from './parameterInput'
    import globalHelpPopupButton from "./globalHelpPopupButton";
    export default {
        name: "parameters",
        components: {
            parameterInput,
            globalHelpPopupButton
        },
        props: {
            parameters: Array
        },
        methods: {
            emptyChanged: function (parameter_name, empty) {
                this.$refs.parameter_input.forEach(
                    input => input.otherEmptyChanged(parameter_name, empty)
                );
            },
            gatherParameters() {
                if (this.$refs.parameter_input === undefined) {
                    return []
                }
                const inputs_with_values = this.$refs.parameter_input.filter(input => input.hasValue());
                return inputs_with_values.map(input => {
                    return {
                        name: input.name,
                        value: input.value
                    }
                });
            }
        },

    }
</script>

<style scoped>

</style>