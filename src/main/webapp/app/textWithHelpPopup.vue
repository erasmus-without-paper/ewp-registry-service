<template>
    <div>
        <div :class="textClass">
            {{ text }}&nbsp;<help-button @click="showPopup()"></help-button>
        </div>
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
            text: String,
            textClass: String
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