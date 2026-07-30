<template>
  <div class="product-image-shell">
    <el-image
      v-if="src"
      :src="src"
      :fit="fit"
      :preview-src-list="preview ? [src] : []"
      class="product-image-shell__image"
    >
      <template #error>
        <div class="product-image-shell__fallback" :style="fallbackStyle">
          <span>{{ shortName }}</span>
        </div>
      </template>
    </el-image>

    <div v-else class="product-image-shell__fallback" :style="fallbackStyle">
      <span>{{ shortName }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  src: {
    type: String,
    default: ''
  },
  name: {
    type: String,
    default: ''
  },
  fit: {
    type: String,
    default: 'cover'
  },
  preview: {
    type: Boolean,
    default: false
  }
})

const gradients = [
  'linear-gradient(135deg, #6b655b 0%, #d2aa7d 100%)',
  'linear-gradient(135deg, #37556b 0%, #6e95b4 100%)',
  'linear-gradient(135deg, #4d8f73 0%, #84c3a6 100%)',
  'linear-gradient(135deg, #9b4d48 0%, #d69074 100%)'
]

const shortName = computed(() => {
  const value = props.name?.trim() || '咖啡'
  return value.slice(0, 2).toUpperCase()
})

const fallbackStyle = computed(() => {
  const seed = `${props.name || ''}${props.src || ''}`
  const hash = Array.from(seed).reduce((sum, char) => sum + char.charCodeAt(0), 0)
  return {
    background: gradients[hash % gradients.length]
  }
})
</script>

<style scoped>
.product-image-shell,
.product-image-shell__image,
.product-image-shell__fallback {
  width: 100%;
  height: 100%;
}

.product-image-shell {
  display: block;
}

.product-image-shell__fallback {
  display: grid;
  place-items: center;
  color: rgba(255, 250, 244, 0.92);
  font-size: 1rem;
  font-weight: var(--font-semibold);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
</style>
