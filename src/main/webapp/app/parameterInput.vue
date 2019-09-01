<template>
    <tr>
        <td class="api-entry-parameter-name" v-if="description">
            <text-with-help-popup :text="name" :popup-name="name" :popup-content="description"></text-with-help-popup>
        </td>
        <td class="api-entry-parameter-name" v-else>
            {{name}}
        </td>
        <td class="api-entry-parameter-value"> <input type="text" :disabled=areDependenciesNotMet v-model="value"> </td>
    </tr>
</template>

<script>
    import textWithHelpPopup from "./textWithHelpPopup";

    export default {
        name: "parameterInput",
        props: {
            name: String,
            dependencies: Array,
            blockers: Array,
            description: String
        },
        components: {
            textWithHelpPopup
        },
        data: function() {
            return {
                dependencyMap: this.createDependencyMap(),
                inputValue: ''
            }
        },
        methods: {
            otherEmptyChanged: function(parameter_name, empty) {
                if (this.dependencies.indexOf(parameter_name) !== -1) {
                    this.dependencyMap[parameter_name] = !empty;
                }
                else if (this.blockers.indexOf(parameter_name) !== -1) {
                    this.dependencyMap[parameter_name] = empty;
                }
            },
            hasValue: function() {
                return this.areDependenciesMet() && this.value !== '';
            },
            createDependencyMap: function() {
                const blockersValues = this.blockers.reduce((map, obj) => {map[obj] = true; return map}, {})
                const dependenciesValues = this.dependencies.reduce((map, obj) => {map[obj] = false; return map}, {})
                return Object.assign({}, blockersValues, dependenciesValues);
            },
            areDependenciesMet: function() {
                return Object.values(this.dependencyMap).every((x) => x);
            }
        },
        computed: {
            areDependenciesNotMet: function() {
                return !this.areDependenciesMet();
            },
            value: {
                get: function() {
                    return this.inputValue;
                },
                set: function(value) {
                    if (value && !this.inputValue) {
                        this.$emit('emptyChanged', this.name, false);
                    } else if (!value && this.inputValue) {
                        this.$emit('emptyChanged', this.name, true);
                    }
                    this.inputValue = value;
                }
            }
        }
    }
</script>

<style scoped>

</style>