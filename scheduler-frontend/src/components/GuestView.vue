<script setup lang="ts">
import { ref, onMounted } from 'vue'
import schedulerApi, { type TimeSlot } from '../services/schedulerApi'

// State
const currentPath = window.location.pathname
const guestToken = currentPath.split('/meet/')[1] || ""
const isTokenExpired = ref(false)
const isBookingSuccess = ref(false)
const selectedDate = ref<string>(new Date().toISOString().split('T')[0] || "")
const availableSlots = ref<TimeSlot[]>([])
const loading = ref(false)
const errorMsg = ref('')
const showModal = ref(false)
const submitting = ref(false)
const form = ref({ title: '', description: '', requesterName: '', requesterEmail: '', startTime: '', endTime: '' })

// Logic
const handleCheckGuest = async () => {
  loading.value = true; errorMsg.value = ''; availableSlots.value = []
  try {
    const slots = await schedulerApi.checkGuestAvailability(guestToken, selectedDate.value)
    if(slots.length === 0) errorMsg.value = "Tidak ada jadwal tersedia."; else availableSlots.value = slots
  } catch (e: any) {
    if (e.response && (e.response.status === 403 || e.response.status === 500)) isTokenExpired.value = true
    else errorMsg.value = "Gagal memuat jadwal."
  } finally { loading.value = false }
}

const openBooking = (slotLabel: string) => {
  const [s, e] = slotLabel.split(' - ')
  form.value.startTime = `${selectedDate.value}T${s}:00`
  form.value.endTime = `${selectedDate.value}T${e}:00`
  showModal.value = true
}

const handleBooking = async () => {
  submitting.value = true
  try {
    await schedulerApi.createAppointment({ ...form.value, shareToken: guestToken })
    showModal.value = false; isBookingSuccess.value = true
  } catch (e: any) {
    if(e.response && e.response.status === 403) isTokenExpired.value = true
    else alert("Gagal booking.")
  } finally { submitting.value = false }
}

onMounted(() => { handleCheckGuest() })
</script>

<template>
  <div class="max-w-lg mx-auto min-h-screen flex flex-col justify-center p-6">
      
      <div v-if="isBookingSuccess" class="bg-white rounded-[3rem] p-12 shadow-2xl shadow-emerald-100 border border-emerald-50 text-center animate-in">
          <div class="inline-block p-6 bg-emerald-50 rounded-full mb-6 text-6xl">✅</div>
          <h1 class="text-3xl font-black text-slate-800 mb-4">Permintaan Terkirim!</h1>
          <p class="text-slate-500 mb-8 leading-relaxed">Terima kasih telah menjadwalkan pertemuan.<br>Notifikasi konfirmasi akan dikirim ke email Anda.</p>
          <div class="p-4 bg-slate-50 rounded-2xl text-xs font-bold text-slate-400 uppercase tracking-widest">Link hangus.</div>
      </div>

      <div v-else-if="isTokenExpired" class="bg-white rounded-[3rem] p-12 shadow-2xl shadow-red-100 border border-red-50 text-center animate-in">
          <div class="inline-block p-6 bg-red-50 rounded-full mb-6 text-6xl">🚫</div>
          <h1 class="text-3xl font-black text-slate-800 mb-4">Link Tidak Aktif</h1>
          <p class="text-slate-500 mb-8 leading-relaxed">Tautan ini sudah digunakan atau kadaluarsa.</p>
          <div class="p-4 bg-slate-50 rounded-2xl text-xs font-bold text-slate-400 uppercase tracking-widest">Sesi Berakhir</div>
      </div>

      <div v-else class="bg-white rounded-[3rem] p-10 md:p-12 shadow-2xl shadow-indigo-100 border border-slate-50 animate-in">
        <div class="text-center mb-10"><div class="inline-block p-4 bg-indigo-50 rounded-3xl mb-4 text-4xl">👋</div><h1 class="text-3xl font-black mb-2 tracking-tight text-slate-800">Atur Pertemuan</h1><p class="text-slate-400 font-medium text-sm">Pilih tanggal untuk melihat ketersediaan.</p></div>
        <div class="space-y-4">
          <div class="relative"><input type="date" v-model="selectedDate" class="w-full p-5 bg-slate-50 rounded-3xl font-bold outline-none ring-2 ring-transparent focus:ring-indigo-500 focus:bg-white transition-all cursor-pointer text-slate-700" /><span class="absolute right-6 top-1/2 -translate-y-1/2 text-xl pointer-events-none">📅</span></div>
          <button @click="handleCheckGuest" :disabled="loading" class="w-full bg-indigo-600 text-white py-5 rounded-3xl font-black shadow-xl shadow-indigo-200 hover:bg-indigo-700 hover:-translate-y-1 transition-all disabled:opacity-50 disabled:translate-y-0 disabled:shadow-none">{{ loading ? 'Mencari Slot...' : 'Cek Ketersediaan' }}</button>
        </div>
        <div v-if="errorMsg" class="mt-6 text-center text-red-500 font-bold p-4 bg-red-50 rounded-2xl text-xs border border-red-100">{{ errorMsg }}</div>
        <div class="grid grid-cols-1 gap-3 mt-8 max-h-[300px] overflow-y-auto pr-2 custom-scrollbar">
          <div v-if="availableSlots.length > 0" class="grid grid-cols-2 gap-3">
            <button v-for="slot in availableSlots" :key="slot.startTime" @click="openBooking(slot.label)" :class="[slot.isRecommended ? 'bg-indigo-50 border-indigo-200 text-indigo-700 ring-1 ring-indigo-200 hover:bg-indigo-100' : 'bg-white border-slate-200 text-slate-600 hover:border-indigo-400 hover:text-indigo-600']" class="relative p-4 border rounded-2xl text-left transition-all active:scale-95 shadow-sm group">
              <div v-if="slot.isRecommended" class="absolute -top-2 -right-2 bg-amber-400 text-white text-[9px] font-black px-2 py-0.5 rounded-full shadow-sm flex items-center gap-1"><span>★</span> BEST</div>
              <div class="font-black text-sm">{{ slot.label }}</div>
              <div class="text-[10px] mt-1 font-medium opacity-70 flex items-center gap-1"><span v-if="slot.isRecommended">⚡ Instant</span><span v-else>☕ Request</span></div>
            </button>
          </div>
        </div>
      </div>

      <div v-if="showModal" class="fixed inset-0 bg-slate-900/40 backdrop-blur-md flex items-center justify-center p-6 z-[999] animate-in"><div class="bg-white w-full max-w-sm rounded-[3rem] p-8 md:p-10 shadow-2xl border border-white/20"><h2 class="text-2xl font-black mb-8 text-slate-800">Detail Anda</h2><form @submit.prevent="handleBooking" class="space-y-4"><input v-model="form.requesterName" placeholder="Nama Lengkap" required class="w-full p-4 bg-slate-50 rounded-2xl font-bold outline-none focus:ring-2 ring-indigo-500" /><input v-model="form.requesterEmail" type="email" placeholder="Email Aktif" required class="w-full p-4 bg-slate-50 rounded-2xl font-bold outline-none focus:ring-2 ring-indigo-500" /><input v-model="form.title" placeholder="Agenda Pertemuan" required class="w-full p-4 bg-slate-50 rounded-2xl font-bold outline-none focus:ring-2 ring-indigo-500" /><div class="flex gap-3 pt-6"><button @click="showModal = false" type="button" class="flex-1 py-4 font-bold text-slate-400 hover:bg-slate-50 rounded-2xl transition-colors">Batal</button><button type="submit" :disabled="submitting" class="flex-1 bg-indigo-600 text-white py-4 rounded-2xl font-black shadow-lg shadow-indigo-100 hover:bg-indigo-700">{{ submitting ? '...' : 'Booking' }}</button></div></form></div></div>
  </div>
</template>