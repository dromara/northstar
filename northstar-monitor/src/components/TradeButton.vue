<template>
  <div
    class="ns-button"
    :class="reverseColor ? 'reverse-color' : ''"
    :style="{ 'background-color': color }"
  >
    <div class="ns-button__wrapper">
      <div class="ns-button__label ns-button--text-mid">
        <span>{{ price }}</span>
      </div>
      <div class="ns-button__divider"></div>
      <div class="ns-button__label ns-button--text-mid">
        <span>{{ label }}</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    price: {
      type: String,
      default: '0'
    },
    label: {
      type: String,
      default: '默认'
    },
    color: {
      type: String,
      default: 'gainsboro'
    },
    reverseColor: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {}
  }
}
</script>

<style>
.ns-button {
  display: flex;
  flex-direction: column;
  min-height: 50px;
  max-height: 120px;
  width: 100%;
  text-align: center;
  border-radius: 10px;
}
.ns-button:active {
  background-color: rgb(100, 100, 100);
  opacity: 0.5;
}
.ns-button__wrapper {
  margin: 10px;
  pointer-events: none;
  user-select: none;
}
.ns-button__label {
  height: 100%;
  font-size: 16px;
}
.ns-button--text-mid {
  display: flex;
  align-items: center;
  justify-content: center;
}
.ns-button__divider {
  border-top: 1px solid gainsboro;
  height: 1px;
}

.normal-color {
  color: gainsboro;
  border-color: gainsboro;
}

.reverse-color .ns-button__divider {
  border-top: 1px solid black;
}

.reverse-color {
  color: black;
  border-color: black;
}
</style>
