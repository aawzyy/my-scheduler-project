<script setup lang="ts">
import { computed } from 'vue'

// Terima data booking dari App.vue
const props = defineProps<{
  bookings: any[]
}>()

// Kirim sinyal ke App.vue kalau tombol ditekan
const emit = defineEmits(['approve', 'reject'])

// Filter hanya yang statusnya PENDING
const pendingRequests = computed(() => {
  return props.bookings
    .filter(b => b.status === 'PENDING')
    .sort((a, b) => a.startTime.localeCompare(b.startTime))
})
</script>

<template>
  <div v-if="pendingRequests.length > 0" class="bg-amber-50 rounded-[2rem] p-6 border border-amber-100 shadow-sm relative overflow-hidden mb-6 animate-in">
    <div class="absolute top-0 right-0 p-4 opacity-10 text-amber-600"><span class="text-9xl">🔔</span></div>
    <div class="relative z-10">
        <h2 class="text-lg font-black text-amber-800 flex items-center gap-2">
            <span>🔔</span> Butuh Persetujuan ({{ pendingRequests.length }})
        </h2>
        <p class="text-xs font-medium text-amber-600/80 mb-4">Ada tamu yang menunggu konfirmasi Anda.</p>
        
        <div class="space-y-3 max-h-[300px] overflow-y-auto pr-2 custom-scrollbar">
            <div v-for="req in pendingRequests" :key="req.id" class="bg-white p-4 rounded-2xl border border-amber-100 shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                    <div class="flex items-center gap-2 mb-1">
                        <span class="text-[10px] font-black bg-slate-100 text-slate-600 px-2 py-1 rounded-md uppercase tracking-wider">
                            📅 {{ new Date(req.startTime).toLocaleDateString('id-ID', { day: 'numeric', month: 'short' }) }}
                        </span>
                        <span class="text-[10px] font-black bg-slate-100 text-slate-600 px-2 py-1 rounded-md uppercase tracking-wider">
                            ⏰ {{ req.startTime.split('T')[1].substring(0,5) }}
                        </span>
                    </div>
                    <h3 class="font-bold text-slate-800 text-sm">{{ req.title }}</h3>
                    <p class="text-xs text-slate-500">Oleh: <span class="font-bold">{{ req.requesterName }}</span></p>
                </div>
                <div class="flex gap-2 w-full sm:w-auto">
                    <button @click="$emit('reject', req.id)" class="flex-1 sm:flex-none px-4 py-2 rounded-xl border border-red-100 text-red-500 text-xs font-bold hover:bg-red-50 transition-colors">Tolak</button>
                    <button @click="$emit('approve', req.id)" class="flex-1 sm:flex-none px-6 py-2 rounded-xl bg-indigo-600 text-white text-xs font-bold shadow-lg shadow-indigo-200 hover:bg-indigo-700 transition-colors">Terima</button>
                </div>
            </div>
        </div>
    </div>
  </div>
</template>