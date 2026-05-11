<template>
  <button
    :class="buttonClasses"
    :disabled="disabled"
    v-bind="$attrs"
    @click="$emit('click', $event)"
  >
    <slot />
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'secondary',
    validator: (v) => [
      'primary', 'outline', 'secondary', 'danger',
      'danger-outline', 'text', 'text-secondary',
      'icon', 'link'
    ].includes(v),
  },
  size: {
    type: String,
    default: 'medium',
    validator: (v) => ['mini', 'small', 'medium', 'large'].includes(v),
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['click'])

const buttonClasses = computed(() => {
  const classes = ['ms-btn', `ms-btn--${props.type}`]
  if (props.size !== 'medium') {
    classes.push(`ms-btn--${props.size}`)
  }
  if (props.disabled) {
    classes.push('is-disabled')
  }
  return classes
})
</script>
