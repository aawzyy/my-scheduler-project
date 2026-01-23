<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import GuestView from './components/GuestView.vue'
import OwnerDashboard from './components/OwnerDashboard.vue'
import schedulerApi from './services/schedulerApi'

const currentPath = window.location.pathname
const isGuestMode = computed(() => currentPath.startsWith('/meet/'))
const isOwnerLoggedIn = ref(false)

const loginGoogle = () => {
  window.location.href = '/oauth2/authorization/google'
}

onMounted(async () => {
  if (!isGuestMode.value) {
    try {
      await schedulerApi.getAppointments()
      isOwnerLoggedIn.value = true
    } catch {
      isOwnerLoggedIn.value = false
    }
  }
})
</script>

<template>
  <div class="min-h-screen bg-[#F8FAFC] font-sans text-slate-900 selection:bg-indigo-100">
    
    <GuestView v-if="isGuestMode" />

    <div v-else>
      <div v-if="!isOwnerLoggedIn" class="flex flex-col items-center justify-center h-screen text-center p-6 bg-white/50 backdrop-blur-sm">
        <div class="w-24 h-24 bg-gradient-to-tr from-indigo-500 to-violet-500 rounded-[2rem] flex items-center justify-center text-5xl shadow-xl shadow-indigo-200 mb-8 transform hover:scale-110 transition-transform duration-500">🗓️</div>
        <h1 class="text-5xl font-black mb-4 tracking-tight text-slate-800">Personal<span class="text-indigo-600">Scheduler</span>.</h1>
        <button @click="loginGoogle" class="bg-white border border-slate-200 px-8 py-4 rounded-2xl font-bold flex gap-4 shadow-lg hover:shadow-xl group"><img src="https://www.svgrepo.com/show/355037/google-icon.svg" class="w-6 h-6"> <span class="text-slate-700">Masuk dengan Google</span></button>
      </div>

      <OwnerDashboard v-else />
    </div>

  </div>
</template>

<style>
@tailwind base; @tailwind components; @tailwind utilities;
.animate-in { animation: fadeIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(15px); scale: 0.98; } to { opacity: 1; transform: translateY(0); scale: 1; } }
.custom-scrollbar::-webkit-scrollbar { width: 5px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #94a3b8; }
</style>