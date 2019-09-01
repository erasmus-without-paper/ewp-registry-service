<template>
    <label>
        <input type="radio"
               :name="'security-' + group"
               :value="name"
               @click="onSelected(name)">
        {{name}}
        <br />
    </label>
</template>

<script>
    export default {
        name: "securityRadioButton",
        props: {
            name: String,
            group: String
        },
        methods: {
            onSelected: function(value) {
                this.$emit('selected', value);
            }
        },
    }
</script>

<style scoped>

</style>