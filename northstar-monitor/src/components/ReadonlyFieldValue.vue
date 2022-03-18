<template>
  <div class="ns-field">
    <div class="ns-field__label" :style="{ width: labelWidth }">{{ label }}</div>
    <div class="ns-field__value">{{ value || '-' }}</div>
  </div>
</template>

<script>
export default {
  props: {
    label: {
      type: String,
      value: ''
    },
    value: {
      type: String,
      value: ''
    },
    labelWidth: {
      type: String,
      value: ''
    }
  }
}
</script>

<style scoped>
.ns-field {
  display: flex;
}
.ns-field div {
  padding-top: 10px;
  padding-right: 10px;
}
.ns-field__value {
  flex: 1;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}
</style>
