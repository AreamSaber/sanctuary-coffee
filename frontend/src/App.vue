<template>
  <div class="app-scene" :style="sceneStyle">
    <div class="app-scene__backdrop" aria-hidden="true">
      <div class="app-scene__layer app-scene__layer--sharp"></div>
      <div class="app-scene__layer app-scene__layer--blur"></div>
      <div class="app-scene__layer app-scene__layer--wash"></div>
      <div class="app-scene__layer app-scene__layer--noise"></div>
      <div v-if="pointerEnabled" class="app-scene__cursor"></div>
    </div>

    <div class="app-scene__content">
      <router-view v-slot="{ Component, route }">
        <AppShell v-if="Component && route.meta?.requiresAuth">
          <component :is="Component" />
        </AppShell>
        <component :is="Component" v-else-if="Component" />
      </router-view>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import AppShell from '@/components/layout/AppShell.vue'

const pointerEnabled = ref(false)
const pointerState = ref({
  pointerX: 50,
  pointerY: 38,
  glowX: 50,
  glowY: 38
})

let animationFrameId = 0
let interactionMediaQuery = null
let targetX = 50
let targetY = 38
let glowX = 50
let glowY = 38

const sceneStyle = computed(() => ({
  '--pointer-x': `${pointerState.value.pointerX}%`,
  '--pointer-y': `${pointerState.value.pointerY}%`,
  '--cursor-x': `${pointerState.value.glowX}%`,
  '--cursor-y': `${pointerState.value.glowY}%`
}))

const animatePointer = () => {
  glowX += (targetX - glowX) * 0.12
  glowY += (targetY - glowY) * 0.12

  pointerState.value = {
    pointerX: targetX,
    pointerY: targetY,
    glowX,
    glowY
  }

  animationFrameId = window.requestAnimationFrame(animatePointer)
}

const stopPointerAnimation = () => {
  window.cancelAnimationFrame(animationFrameId)
  animationFrameId = 0
}

const handlePointerMove = (event) => {
  targetX = (event.clientX / window.innerWidth) * 100
  targetY = (event.clientY / window.innerHeight) * 100
}

const syncPointerCapability = () => {
  pointerEnabled.value = interactionMediaQuery?.matches ?? false

  if (pointerEnabled.value) {
    window.addEventListener('pointermove', handlePointerMove)
    if (!animationFrameId) {
      animatePointer()
    }
    return
  }

  window.removeEventListener('pointermove', handlePointerMove)
  stopPointerAnimation()
  targetX = 50
  targetY = 38
  glowX = 50
  glowY = 38
  pointerState.value = {
    pointerX: 50,
    pointerY: 38,
    glowX: 50,
    glowY: 38
  }
}

onMounted(() => {
  interactionMediaQuery = window.matchMedia('(pointer: fine) and (prefers-reduced-motion: no-preference)')
  syncPointerCapability()

  if (interactionMediaQuery.addEventListener) {
    interactionMediaQuery.addEventListener('change', syncPointerCapability)
  } else {
    interactionMediaQuery.addListener(syncPointerCapability)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', handlePointerMove)
  stopPointerAnimation()

  if (!interactionMediaQuery) {
    return
  }

  if (interactionMediaQuery.removeEventListener) {
    interactionMediaQuery.removeEventListener('change', syncPointerCapability)
  } else {
    interactionMediaQuery.removeListener(syncPointerCapability)
  }
})
</script>

<style>
.app-scene {
  position: relative;
  min-height: 100vh;
  isolation: isolate;
  overflow-x: hidden;
  --scene-artwork: url('https://images.unsplash.com/photo-1586023492125-27b2c045efd7?q=80&w=2560&auto=format&fit=crop');
}

.app-scene__backdrop {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
}

.app-scene__layer {
  position: absolute;
  inset: 0;
}

.app-scene__layer--sharp,
.app-scene__layer--blur {
  background-image:
    linear-gradient(180deg, rgba(10, 8, 7, 0.2) 0%, rgba(10, 8, 7, 0.48) 100%),
    var(--scene-artwork);
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
}

.app-scene__layer--sharp {
  filter: saturate(0.82) sepia(0.18) contrast(1.04);
  transform: scale(1.01);
}

.app-scene__layer--blur {
  filter: blur(34px) brightness(0.72) saturate(0.88) sepia(0.25);
  transform: scale(1.1);
  mask-image: radial-gradient(circle 320px at var(--pointer-x, 50%) var(--pointer-y, 38%), transparent 0%, rgba(0, 0, 0, 0.08) 40%, black 100%);
  -webkit-mask-image: radial-gradient(circle 320px at var(--pointer-x, 50%) var(--pointer-y, 38%), transparent 0%, rgba(0, 0, 0, 0.08) 40%, black 100%);
}

.app-scene__layer--wash {
  background:
    radial-gradient(circle at 18% 18%, rgba(246, 232, 214, 0.14) 0%, transparent 24%),
    radial-gradient(circle at 82% 14%, rgba(216, 164, 101, 0.12) 0%, transparent 20%),
    linear-gradient(135deg, rgba(25, 18, 14, 0.35) 0%, rgba(106, 96, 84, 0.32) 45%, rgba(10, 8, 7, 0.68) 100%);
}

.app-scene__layer--noise {
  opacity: 0.12;
  mix-blend-mode: soft-light;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.84' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E");
}

.app-scene__cursor {
  position: absolute;
  left: var(--cursor-x, 50%);
  top: var(--cursor-y, 38%);
  width: 18rem;
  height: 18rem;
  transform: translate(-50%, -50%);
  border-radius: 999px;
  background: radial-gradient(circle, rgba(255, 245, 232, 0.18) 0%, rgba(255, 245, 232, 0.06) 36%, transparent 72%);
  filter: blur(10px);
  opacity: 0.8;
}

.app-scene__content {
  position: relative;
  z-index: 1;
  min-height: 100vh;
}

@media (max-width: 768px) {
  .app-scene__layer--blur {
    mask-image: radial-gradient(circle 180px at var(--pointer-x, 50%) var(--pointer-y, 38%), transparent 0%, rgba(0, 0, 0, 0.1) 42%, black 100%);
    -webkit-mask-image: radial-gradient(circle 180px at var(--pointer-x, 50%) var(--pointer-y, 38%), transparent 0%, rgba(0, 0, 0, 0.1) 42%, black 100%);
  }
}
</style>
