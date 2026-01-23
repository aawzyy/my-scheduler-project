<script setup lang="ts">
import { computed, ref } from 'vue'

const props = defineProps<{
  bookings: any[]
}>()

const emit = defineEmits(['approve', 'reject'])

// Referensi ke elemen Container untuk manipulasi scroll
const scrollContainer = ref<HTMLElement | null>(null)

// State untuk fitur Mouse Drag
const isDown = ref(false)
const startX = ref(0)
const scrollLeft = ref(0)

// Filter data
const pendingRequests = computed(() => {
  return props.bookings
    .filter(b => b.status === 'PENDING')
    .sort((a, b) => a.startTime.localeCompare(b.startTime))
})

// --- LOGIC 1: TOMBOL NAVIGASI ---
const scroll = (direction: 'left' | 'right') => {
  if (scrollContainer.value) {
    const scrollAmount = 320 // Geser sejauh lebar satu kartu
    if (direction === 'left') {
      scrollContainer.value.scrollBy({ left: -scrollAmount, behavior: 'smooth' })
    } else {
      scrollContainer.value.scrollBy({ left: scrollAmount, behavior: 'smooth' })
    }
  }
}

// --- LOGIC 2: MOUSE DRAG (SWIPE) ---
const startDrag = (e: MouseEvent) => {
  if (!scrollContainer.value) return
  isDown.value = true
  startX.value = e.pageX - scrollContainer.value.offsetLeft
  scrollLeft.value = scrollContainer.value.scrollLeft
}

const stopDrag = () => {
  isDown.value = false
}

const doDrag = (e: MouseEvent) => {
  if (!isDown.value || !scrollContainer.value) return
  e.preventDefault()
  const x = e.pageX - scrollContainer.value.offsetLeft
  const walk = (x - startX.value) * 2 // Kecepatan scroll (2x lebih cepat)
  scrollContainer.value.scrollLeft = scrollLeft.value - walk
}
</script>

<template>
  <div v-if="pendingRequests.length > 0" class="bg-amber-50/80 backdrop-blur-sm rounded-[2rem] p-6 border border-amber-100 shadow-sm relative mb-6">
    
    <div class="flex justify-between items-end mb-4 relative z-10">
        <div>
            <h2 class="text-lg font-black text-amber-900 flex items-center gap-2">
                <span>🔔</span> Inbox ({{ pendingRequests.length }})
            </h2>
            <p class="text-[11px] font-bold text-amber-600/70 uppercase tracking-wider mt-1 hidden sm:block">
                Tinjau permintaan masuk
            </p>
        </div>
        
        <div class="flex gap-2">
            <button @click="scroll('left')" class="w-10 h-10 flex items-center justify-center bg-white rounded-full shadow-sm border border-amber-100 text-amber-600 hover:bg-amber-100 transition-colors">
                ←
            </button>
            <button @click="scroll('right')" class="w-10 h-10 flex items-center justify-center bg-white rounded-full shadow-sm border border-amber-100 text-amber-600 hover:bg-amber-100 transition-colors">
                →
            </button>
        </div>

        <div class="absolute -top-6 -right-6 text-9xl opacity-5 text-amber-500 pointer-events-none">📬</div>
    </div>

    <div 
        ref="scrollContainer"
        @mousedown="startDrag"
        @mouseleave="stopDrag"
        @mouseup="stopDrag"
        @mousemove="doDrag"
        class="flex gap-4 overflow-x-auto pb-4 px-1 scrollbar-hide cursor-grab active:cursor-grabbing select-none"
    >
        <div v-for="req in pendingRequests" :key="req.id" 
             class="min-w-[280px] w-[85%] sm:w-[320px] bg-white p-5 rounded-3xl border border-amber-100 shadow-sm flex flex-col justify-between group hover:border-amber-300 transition-all flex-shrink-0">
            
            <div class="mb-4 pointer-events-none"> <div class="flex items-center gap-2 mb-2">
                    <span class="text-[10px] font-black bg-slate-100 text-slate-600 px-2 py-1 rounded-lg uppercase tracking-wider">
                        📅 {{ new Date(req.startTime).toLocaleDateString('id-ID', { day: 'numeric', month: 'short' }) }}
                    </span>
                    <span class="text-[10px] font-black bg-slate-100 text-slate-600 px-2 py-1 rounded-lg uppercase tracking-wider">
                        ⏰ {{ req.startTime.split('T')[1].substring(0,5) }}
                    </span>
                </div>
                <h3 class="font-black text-slate-800 text-base leading-tight mb-1">{{ req.title }}</h3>
                <p class="text-xs text-slate-400 font-medium">Oleh: <span class="text-slate-600 font-bold">{{ req.requesterName }}</span></p>
            </div>

            <div class="flex gap-2 mt-auto">
                <button 
                    @click="$emit('reject', req.id)" 
                    class="flex-1 px-3 py-3 rounded-xl border border-red-50 text-red-400 text-xs font-black hover:bg-red-50 hover:text-red-600 transition-colors cursor-pointer relative z-20">
                    Tolak
                </button>
                <button 
                    @click="$emit('approve', req.id)" 
                    class="flex-1 px-3 py-3 rounded-xl bg-slate-900 text-white text-xs font-black shadow-lg shadow-slate-200 hover:bg-indigo-600 transition-all transform active:scale-95 cursor-pointer relative z-20">
                    Terima
                </button>
            </div>
        </div>
    </div>
  </div>
</template>

<style scoped>
/* Sembunyikan scrollbar tapi tetap bisa discroll */
.scrollbar-hide::-webkit-scrollbar {
    display: none;
}
.scrollbar-hide {
    -ms-overflow-style: none;
    scrollbar-width: none;
}
</style>