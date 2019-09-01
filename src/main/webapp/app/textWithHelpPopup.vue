<template>
    <div>
        {{ text }}&nbsp;<help-button @click="showPopup()"></help-button>
        <help-popup ref="popup" :popup-content="popupContent" :popup-name="popupName"></help-popup>
    </div>
</template>

<script>
    import helpPopup from "./helpPopup"
    import helpButton from "./helpButton"

    export default {
        name: "textWithHelpPopup",
        components: {
            helpPopup,
            helpButton
        },
        props: {
            popupContent: String,
            popupName: String,
            text: String
        },
        methods: {
            showPopup: function() {
                this.$refs.popup.showPopup();
            }
        }
    }
</script>

<style scoped>

</style>