<template>
    <help-button @click="showPopup()"></help-button>
</template>

<script>
    import helpButton from "./helpButton";

    export default {
        name: "globalHelpPopupButton",
        components: {
            helpButton
        },
        props: {
            popupId: String
        },
        methods: {
            showPopup: function() {
                this.$root.$refs[this.popupId].showPopup();
            }
        }
    }
</script>

<style scoped>

</style>