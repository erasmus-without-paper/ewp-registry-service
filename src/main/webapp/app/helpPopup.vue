<template>
    <div>
        <div :class='classes'>
            <div class="help-popup-popup">
                <h2>{{ popupName }}</h2>
                <a class="help-popup-close" @click="hidePopup()">&times;</a>
                <div class="help-popup-content">
                    <div v-if="popupContent">
                        {{ popupContent }}
                    </div>
                    <div v-else>
                        <slot></slot>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "helpPopup",
        props: {
            popupName: String,
            popupContent: String
        },
        data: function() {
            return {
                active: false
            }
        },
        computed: {
            classes: function () {
                return {
                    'help-popup-overlay': true,
                    'help-popup-overlay-visible': this.active
                }
            }
        },
        methods: {
            hidePopup: function() {
                this.active = false;
            },
            showPopup: function() {
                this.active = true;
            }
        },
    }
</script>

<style scoped>

</style>