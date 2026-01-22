<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import schedulerApi, { type TimeSlot, type UserContact } from './services/schedulerApi'

// --- STATE UMUM ---
const currentPath = ref(window.location.pathname)
const isGuestMode = computed(() => currentPath.value.startsWith('/meet/'))
const guestToken = computed(() => isGuestMode.value ? currentPath.value.split('/meet/')[1] || "" : "")

// --- STATE KONTROL UI TAMU ---
const isTokenExpired = ref(false)
const isBookingSuccess = ref(false)

// --- STATE OWNER ---
const isOwnerLoggedIn = ref(false)
const activeTab = ref('dashboard')
const ownerBookings = ref<any[]>([]) 
const syncStatus = ref<any>(null)
const generatedLink = ref<any>(null)
const workSchedules = ref<any[]>([])
const weekDays = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

// --- STATE MEMORY LOGS (BARU) ---
const memoryLogs = ref<any[]>([])

// --- STATE SETTINGS & CONTACTS ---
const autoAcceptThreshold = ref(85)
const isSavingConfig = ref(false)
const contacts = ref<UserContact[]>([])
const contactForm = ref<UserContact>({ email: '', name: '', category: 'VIP', priorityScore: 50 })
const isSavingContact = ref(false)

// --- STATE DASHBOARD ---
const today = new Date()
const currentMonth = ref(today.getMonth())
const currentYear = ref(today.getFullYear())
const selectedDashboardDate = ref(today.toISOString().split('T')[0]) 
let pollingInterval: any = null

// --- STATE GUEST FORM ---
const selectedDate = ref<string>(new Date().toISOString().split('T')[0] || "")
const availableSlots = ref<TimeSlot[]>([]) 
const loading = ref(false)
const errorMsg = ref('')

// --- MODALS ---
const showModal = ref(false)
const showRoutineModal = ref(false)
const showTaskModal = ref(false)
const submitting = ref(false)

const form = ref({ title: '', description: '', requesterName: '', requesterEmail: '', startTime: '', endTime: '' })
const routineForm = ref({ name: '', startTime: '', endTime: '' })
const taskForm = ref({ title: '', description: 'Quick task', requesterName: 'Owner', requesterEmail: '', startTime: '', endTime: '' })

// --- INITIALIZATION ---
onMounted(async () => {
  if (!isGuestMode.value) {
    // Mode Owner
    await checkAuth()
    await loadWorkSchedules()
    await loadConfigs()
    await loadContacts()
    await loadLogs() // <--- LOAD MEMORI SAAT AWAL
    
    pollingInterval = setInterval(async () => {
      if (isOwnerLoggedIn.value) {
        try {
          ownerBookings.value = await schedulerApi.getAppointments()
        } catch (e) { console.error("Auto-sync failed", e) }
      }
    }, 5000)
  } else {
    // Mode Guest
    await handleCheckGuest()
  }
})

onUnmounted(() => {
  if (pollingInterval) clearInterval(pollingInterval)
})

// --- LOGIC OWNER ---
const checkAuth = async () => { try { ownerBookings.value = await schedulerApi.getAppointments(); isOwnerLoggedIn.value = true; syncStatus.value = await schedulerApi.getDashboardStatus(); taskForm.value.requesterEmail = syncStatus.value?.email || '' } catch (e) { isOwnerLoggedIn.value = false } }
const loginGoogle = () => window.location.href = '/oauth2/authorization/google'

const loadConfigs = async () => { try { const configs = await schedulerApi.getAllConfigs(); const thresholdConfig = configs.find((c: any) => c.configKey === 'AUTO_ACCEPT_THRESHOLD'); if (thresholdConfig) autoAcceptThreshold.value = parseInt(thresholdConfig.configValue) } catch (e) { console.error("Gagal load config") } }
const saveBrainConfig = async () => { isSavingConfig.value = true; try { await schedulerApi.updateConfig('AUTO_ACCEPT_THRESHOLD', autoAcceptThreshold.value.toString()); alert("Otak asisten berhasil di-update!") } catch (e) { alert("Gagal menyimpan") } finally { isSavingConfig.value = false } }

const loadContacts = async () => { try { contacts.value = await schedulerApi.getContacts() } catch (e) { console.error("Gagal load kontak") } }
const handleAddContact = async () => { isSavingContact.value = true; try { await schedulerApi.addContact(contactForm.value); await loadContacts(); contactForm.value = { email: '', name: '', category: 'VIP', priorityScore: 50 }; alert("Kontak tersimpan") } catch (e) { alert("Gagal") } finally { isSavingContact.value = false } }
const handleDeleteContact = async (id: string) => { if(!confirm("Hapus?")) return; try { await schedulerApi.deleteContact(id); await loadContacts() } catch (e) { alert("Gagal hapus") } }

// --- LOGIC MEMORY LOGS (BARU) ---
const loadLogs = async () => {
  try {
    memoryLogs.value = await schedulerApi.getDecisionLogs()
  } catch (e) {
    console.error("Gagal load logs")
  }
}

const loadWorkSchedules = async () => { try { const fromDb = await schedulerApi.getWorkSchedules(); workSchedules.value = weekDays.map(day => { const existing = fromDb.find((s: any) => s.dayOfWeek === day); return existing || { dayOfWeek: day, workingDay: false, startTime: '09:00:00', endTime: '17:00:00' } }) } catch (e) { console.error(e) } }

const monthNames = ["Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember"]
const calendarGrid = computed(() => { const firstDay = new Date(currentYear.value, currentMonth.value, 1).getDay(); const daysInMonth = new Date(currentYear.value, currentMonth.value + 1, 0).getDate(); const padding = firstDay === 0 ? 6 : firstDay - 1; const days = []; for (let i = 0; i < padding; i++) days.push(null); for (let i = 1; i <= daysInMonth; i++) { const dateStr = `${currentYear.value}-${String(currentMonth.value + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`; days.push({ day: i, date: dateStr }) } return days })
const nextMonth = () => { if (currentMonth.value === 11) { currentMonth.value = 0; currentYear.value++ } else { currentMonth.value++ } }
const prevMonth = () => { if (currentMonth.value === 0) { currentMonth.value = 11; currentYear.value-- } else { currentMonth.value-- } }
const hasAppointment = (dateStr: string) => { return ownerBookings.value.some(b => b.startTime && b.startTime.startsWith(dateStr) && b.status !== 'REJECTED') }
const selectedDayAgenda = computed(() => { return ownerBookings.value.filter(b => b.startTime && b.startTime.startsWith(selectedDashboardDate.value)).sort((a, b) => a.startTime.localeCompare(b.startTime)) })

const handleApprove = async (id: string) => { if(!confirm("Setujui?")) return; try { await schedulerApi.approveAppointment(id); await checkAuth(); await loadLogs() } catch (e) { alert("Gagal") } }
const handleReject = async (id: string) => { if(!confirm("Tolak?")) return; try { await schedulerApi.rejectAppointment(id); await checkAuth(); await loadLogs() } catch (e) { alert("Gagal") } }
const handleQuickTask = async () => { submitting.value = true; try { await schedulerApi.createQuickTask(taskForm.value); showTaskModal.value = false; await checkAuth(); await loadLogs() } catch (e) { alert("Gagal") } finally { submitting.value = false } }

const handleUpdateWorkDay = async (day: string) => { const data = workSchedules.value.find(s => s.dayOfWeek === day); if (!data) return; try { await schedulerApi.updateWorkSchedule(data) } catch (e) { alert("Gagal") } }
const handleAddRoutine = async () => { try { const payload = { ...routineForm.value, startTime: routineForm.value.startTime + ":00", endTime: routineForm.value.endTime + ":00" }; await schedulerApi.addPersonalBlock(payload); showRoutineModal.value = false; syncStatus.value = await schedulerApi.getDashboardStatus() } catch (e) { alert("Gagal") } }
const handleDeleteRoutine = async (id: number) => { if (!confirm("Hapus?")) return; try { await schedulerApi.deletePersonalBlock(id); syncStatus.value = await schedulerApi.getDashboardStatus() } catch (e) { alert("Gagal") } }
const handleGenerateLink = async (type: 'WORK' | 'SOCIAL') => { try { generatedLink.value = await schedulerApi.generateShareLink(type) } catch (e) { alert("Gagal") } }
const copyToClipboard = (text: string) => navigator.clipboard.writeText(text).then(() => alert("Copied!"))

// ==========================================
// LOGIC TAMU (GUEST)
// ==========================================
const handleCheckGuest = async () => {
  loading.value = true; 
  errorMsg.value = ''; 
  availableSlots.value = []
  
  try {
    const slots = await schedulerApi.checkGuestAvailability(guestToken.value, selectedDate.value)
    if(slots.length === 0) errorMsg.value = "Tidak ada jadwal tersedia di tanggal ini."
    else availableSlots.value = slots
  } catch (e: any) { 
    if (e.response && (e.response.status === 403 || e.response.status === 500)) {
        const msg = e.response.data || "";
        if (typeof msg === 'string' && (msg.includes("DIPAKAI") || msg.includes("KADALUARSA") || msg.includes("Expired") || msg.includes("tidak valid") || e.response.status === 403)) {
             isTokenExpired.value = true; 
        } else {
             errorMsg.value = "Gagal memuat jadwal. Link mungkin bermasalah."
        }
    } else {
        errorMsg.value = "Terjadi kesalahan koneksi."
    }
  } finally { 
    loading.value = false 
  }
}

const openBooking = (slotLabel: string) => { 
  const [s, e] = slotLabel.split(' - '); 
  form.value.startTime = `${selectedDate.value}T${s}:00`; 
  form.value.endTime = `${selectedDate.value}T${e}:00`; 
  showModal.value = true 
}

const handleBooking = async () => { 
  submitting.value = true; 
  try { 
    const payload = { ...form.value, shareToken: guestToken.value }
    await schedulerApi.createAppointment(payload); 
    showModal.value = false; 
    isBookingSuccess.value = true; 
    isTokenExpired.value = true; 
  } catch (e: any) { 
    if(e.response && e.response.status === 403) {
        alert("Maaf! Link ini baru saja hangus/dipakai.")
        isTokenExpired.value = true;
    } else {
        alert("Gagal booking. Coba lagi.") 
    }
  } finally { 
    submitting.value = false 
  } 
}
</script>

<template>
  <div class="min-h-screen bg-[#F8FAFC] font-sans text-slate-900 selection:bg-indigo-100">
    
    <div v-if="!isGuestMode">
      <div v-if="!isOwnerLoggedIn" class="flex flex-col items-center justify-center h-screen text-center p-6 bg-white/50 backdrop-blur-sm">
        <div class="w-24 h-24 bg-gradient-to-tr from-indigo-500 to-violet-500 rounded-[2rem] flex items-center justify-center text-5xl shadow-xl shadow-indigo-200 mb-8 transform hover:scale-110 transition-transform duration-500">🗓️</div>
        <h1 class="text-5xl font-black mb-4 tracking-tight text-slate-800">Personal<span class="text-indigo-600">Scheduler</span>.</h1>
        <button @click="loginGoogle" class="bg-white border border-slate-200 px-8 py-4 rounded-2xl font-bold flex gap-4 shadow-lg hover:shadow-xl group"><img src="https://www.svgrepo.com/show/355037/google-icon.svg" class="w-6 h-6"> <span class="text-slate-700">Masuk dengan Google</span></button>
      </div>

      <div v-else class="max-w-[1400px] mx-auto p-6 md:p-10">
        <header class="flex flex-col md:flex-row justify-between items-center gap-6 mb-10 animate-in">
            <div><h1 class="text-3xl font-black tracking-tight text-slate-800">Dashboard</h1><p class="text-slate-400 font-medium text-sm mt-1">Selamat datang kembali!</p></div>
            <div class="flex items-center gap-4 bg-white p-2 rounded-2xl shadow-sm border border-slate-100">
               <div v-if="syncStatus" class="flex items-center gap-2.5 px-4 py-2 bg-slate-50 rounded-xl border border-slate-100"><span class="relative flex h-2.5 w-2.5"><span :class="syncStatus.isExpired ? 'bg-red-400' : 'bg-emerald-400'" class="animate-ping absolute inline-flex h-full w-full rounded-full opacity-75"></span><span :class="syncStatus.isExpired ? 'bg-red-500' : 'bg-emerald-500'" class="relative inline-flex rounded-full h-2.5 w-2.5"></span></span><span class="text-[11px] font-bold uppercase tracking-wider text-slate-500">{{ syncStatus.isExpired ? 'Offline' : 'Live Sync' }}</span></div>
               <nav class="flex bg-slate-100/50 p-1 rounded-xl">
                  <button v-for="t in ['dashboard', 'routines', 'share', 'settings', 'contacts', 'logs']" :key="t" @click="activeTab = t" :class="activeTab === t ? 'bg-white shadow-sm text-indigo-600 ring-1 ring-black/5' : 'text-slate-500 hover:text-slate-700'" class="px-5 py-2 rounded-lg text-[11px] font-black uppercase tracking-wide transition-all duration-300">{{ t }}</button>
               </nav>
            </div>
        </header>

        <div v-if="activeTab === 'dashboard'" class="grid grid-cols-1 lg:grid-cols-12 gap-6 animate-in">
            <div class="lg:col-span-7 bg-white rounded-[2rem] p-6 border border-slate-100 shadow-sm h-fit">
               <div class="flex justify-between items-end mb-6">
                  <div><h2 class="text-2xl font-black text-slate-800">{{ monthNames[currentMonth] }}</h2><p class="text-slate-400 font-bold text-sm">{{ currentYear }}</p></div>
                  <div class="flex gap-2"><button @click="prevMonth" class="w-8 h-8 flex items-center justify-center rounded-full border border-slate-100 hover:bg-slate-50">←</button><button @click="nextMonth" class="w-8 h-8 flex items-center justify-center rounded-full border border-slate-100 hover:bg-slate-50">→</button></div>
               </div>
               <div class="grid grid-cols-7 gap-2 text-center mb-4"><div v-for="d in ['Sn', 'Sl', 'Rb', 'Km', 'Jm', 'Sb', 'Mg']" :key="d" class="text-[10px] font-black text-slate-300 uppercase tracking-widest">{{ d }}</div></div>
               <div class="grid grid-cols-7 gap-1.5"><div v-for="(day, idx) in calendarGrid" :key="idx" class="aspect-square"><div v-if="day" @click="selectedDashboardDate = day.date" :class="[selectedDashboardDate === day.date ? 'bg-indigo-600 text-white shadow-md' : 'hover:bg-indigo-50 text-slate-600', hasAppointment(day.date) ? 'font-bold' : 'font-medium']" class="w-full h-full rounded-2xl flex flex-col items-center justify-center cursor-pointer transition-all relative group"><span class="text-xs z-10">{{ day.day }}</span><div v-if="hasAppointment(day.date)" :class="selectedDashboardDate === day.date ? 'bg-white' : 'bg-indigo-400'" class="w-1 h-1 rounded-full mt-1"></div></div></div></div>
            </div>
            <div class="lg:col-span-5 bg-white rounded-[2rem] p-6 border border-slate-100 shadow-sm h-fit min-h-[500px] flex flex-col">
               <div class="flex justify-between items-center mb-6"><div><h2 class="text-lg font-black text-slate-800">Agenda Harian</h2><p class="text-[10px] font-bold text-slate-400 mt-0.5 uppercase tracking-widest">📅 {{ selectedDashboardDate }}</p></div><button @click="showTaskModal = true" class="bg-indigo-600 text-white text-[10px] font-black uppercase tracking-wider px-3 py-2 rounded-xl shadow-lg hover:bg-indigo-700 flex items-center gap-1">+ Task</button></div>
               <div class="flex-1 overflow-y-auto pr-2 custom-scrollbar">
                 <div v-if="selectedDayAgenda.length > 0" class="space-y-4">
                    <div v-for="agenda in selectedDayAgenda" :key="agenda.id" class="bg-white rounded-2xl p-4 border border-slate-100 shadow-sm hover:border-indigo-100 transition-all">
                          <div class="flex justify-between items-start mb-2"><span class="text-[10px] font-black px-2 py-1 rounded-md bg-indigo-50 text-indigo-600">{{ agenda.startTime.split('T')[1].substring(0,5) }} - {{ agenda.endTime.split('T')[1].substring(0,5) }}</span>
                            <div v-if="agenda.status === 'GOOGLE'"><img src="https://www.svgrepo.com/show/355037/google-icon.svg" class="w-3.5 h-3.5 opacity-70"></div>
                            <div v-else-if="agenda.status === 'ACCEPTED'"><span class="text-emerald-500 text-[10px] font-bold">● Synced</span></div>
                            <div v-else-if="agenda.status === 'REJECTED'"><span class="text-red-400 text-[10px] font-bold line-through">Ditolak</span></div>
                          </div>
                          <h3 class="font-bold text-slate-800 text-sm leading-snug mb-1">{{ agenda.title }}</h3>
                          <p v-if="agenda.status !== 'GOOGLE'" class="text-[10px] text-slate-400 font-medium">Oleh: {{ agenda.requesterName }}</p>
                          <div v-if="agenda.status === 'PENDING'" class="mt-3 flex gap-2 pt-3 border-t border-slate-50"><button @click="handleReject(agenda.id)" class="flex-1 bg-white border border-red-100 text-red-500 py-1.5 rounded-lg text-[10px] font-bold">Tolak</button><button @click="handleApprove(agenda.id)" class="flex-1 bg-indigo-600 text-white py-1.5 rounded-lg text-[10px] font-bold">Terima</button></div>
                    </div>
                 </div>
                 <div v-else class="flex flex-col items-center justify-center h-48 text-slate-300"><div class="text-4xl mb-2 grayscale opacity-50">☕</div><div class="text-xs font-bold text-slate-400">Kosong.</div></div>
               </div>
            </div>
        </div>
        
        <div v-if="activeTab === 'routines'" class="grid grid-cols-1 lg:grid-cols-3 gap-8 animate-in">
             <div class="lg:col-span-1 bg-white p-8 rounded-[2.5rem] border border-slate-100 shadow-sm h-fit"><h2 class="text-xl font-black mb-6 text-slate-800">💼 Jam Kerja</h2><div v-if="workSchedules.length > 0" class="space-y-3"><div v-for="day in weekDays" :key="day" class="flex items-center justify-between p-3 rounded-2xl border border-transparent hover:border-slate-100"><div class="flex items-center gap-3"><input type="checkbox" :checked="workSchedules.find(s => s.dayOfWeek === day)?.workingDay" @change="(e:any) => { const s = workSchedules.find(x => x.dayOfWeek === day); if(s) { s.workingDay = e.target.checked; handleUpdateWorkDay(day); }}" class="h-5 w-5 accent-indigo-600"><span class="text-[11px] font-black uppercase tracking-widest text-slate-400 w-8">{{ day.substring(0,3) }}</span></div><div v-if="workSchedules.find(s => s.dayOfWeek === day)?.workingDay" class="flex items-center gap-1.5"><input type="time" v-model="workSchedules.find(s => s.dayOfWeek === day).startTime" @change="handleUpdateWorkDay(day)" class="bg-slate-100 p-1.5 rounded-lg text-[11px] font-bold w-16 text-center" /><span>-</span><input type="time" v-model="workSchedules.find(s => s.dayOfWeek === day).endTime" @change="handleUpdateWorkDay(day)" class="bg-slate-100 p-1.5 rounded-lg text-[11px] font-bold w-16 text-center" /></div></div></div></div>
             <div class="lg:col-span-2"><div class="bg-white p-8 rounded-[2.5rem] border border-slate-100 shadow-sm min-h-[400px]"><div class="flex justify-between items-center mb-6"><h2 class="text-xl font-black text-slate-800">⛔ Blokir Waktu</h2><button @click="showRoutineModal = true" class="bg-slate-900 text-white px-4 py-2 rounded-xl text-xs font-bold">+ Baru</button></div><div class="grid grid-cols-1 md:grid-cols-2 gap-4"><div v-for="pb in syncStatus?.personalBlocks" :key="pb.id" class="bg-slate-50 p-6 rounded-[2rem] flex justify-between items-start"><div><div class="font-black text-slate-400 text-[10px] uppercase mb-2">{{ pb.name }}</div><div class="text-3xl font-black text-slate-800">{{ pb.startTime.substring(0,5) }} - {{ pb.endTime.substring(0,5) }}</div></div><button @click="handleDeleteRoutine(pb.id)" class="text-red-300 hover:text-red-500">✕</button></div></div></div></div>
        </div>

        <div v-if="activeTab === 'share'" class="grid md:grid-cols-2 gap-8 animate-in max-w-4xl mx-auto"><div v-for="mode in ['WORK', 'SOCIAL']" :key="mode" class="bg-white p-10 rounded-[3rem] border border-slate-100 shadow-sm flex flex-col items-center text-center"><h2 class="text-2xl font-black mb-2 text-slate-800">{{ mode }} Mode</h2><button @click="handleGenerateLink(mode as any)" class="w-full bg-slate-50 text-slate-900 py-4 rounded-2xl font-black hover:bg-indigo-600 hover:text-white transition-all mt-4">Generate Link</button></div><div v-if="generatedLink" class="col-span-full bg-indigo-600 p-8 rounded-[2.5rem] flex flex-col md:flex-row justify-between items-center text-white font-mono text-sm">{{ generatedLink.shareLink }} <button @click="copyToClipboard(generatedLink.shareLink)" class="bg-white text-indigo-600 px-4 py-2 rounded-xl font-black text-xs">Copy</button></div></div>

        <div v-if="activeTab === 'settings'" class="animate-in max-w-2xl mx-auto">
             <div class="bg-white p-10 rounded-[3.5rem] border border-slate-100 shadow-lg">
                <h2 class="text-3xl font-black mb-6 text-slate-800">🧠 Pengaturan Otak</h2>
                <div class="bg-slate-50 p-8 rounded-[2rem] mb-8">
                    <div class="flex justify-between items-end mb-4"><label class="font-bold text-slate-700">Auto Accept Threshold</label><span class="text-3xl font-black text-indigo-600">{{ autoAcceptThreshold }}</span></div>
                    <input type="range" min="0" max="100" v-model="autoAcceptThreshold" class="w-full h-4 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-indigo-600">
                    <div class="flex justify-between mt-3 text-[10px] font-black uppercase text-slate-400"><span>Terima Semua (0)</span><span>Eksklusif (100)</span></div>
                </div>
                <button @click="saveBrainConfig" :disabled="isSavingConfig" class="w-full bg-slate-900 text-white px-10 py-4 rounded-2xl font-black shadow-xl hover:bg-indigo-600">{{ isSavingConfig ? '...' : 'Simpan Pengaturan' }}</button>
             </div>
        </div>

        <div v-if="activeTab === 'contacts'" class="animate-in max-w-4xl mx-auto grid grid-cols-1 md:grid-cols-3 gap-8">
             <div class="md:col-span-1 bg-white p-8 rounded-[2.5rem] border border-slate-100 shadow-sm h-fit">
                <h2 class="text-xl font-black mb-6 text-slate-800">Tambah Kontak</h2>
                <form @submit.prevent="handleAddContact" class="space-y-4">
                    <input v-model="contactForm.email" type="email" placeholder="Email" required class="w-full p-4 bg-slate-50 rounded-2xl font-bold text-sm outline-none" />
                    <input v-model="contactForm.name" placeholder="Nama Lengkap" required class="w-full p-4 bg-slate-50 rounded-2xl font-bold text-sm outline-none" />
                    <div><label class="text-[10px] font-black text-slate-400 ml-2 uppercase">Kategori</label><select v-model="contactForm.category" class="w-full p-4 bg-slate-50 rounded-2xl font-bold text-sm outline-none"><option value="VIP">VIP (Prioritas)</option><option value="TEAM">Team</option><option value="BLACKLIST">Blacklist</option></select></div>
                    <div><label class="text-[10px] font-black text-slate-400 ml-2 uppercase">Bonus Skor</label><input type="number" v-model="contactForm.priorityScore" class="w-full p-4 bg-slate-50 rounded-2xl font-bold text-sm outline-none" /></div>
                    <button type="submit" :disabled="isSavingContact" class="w-full bg-indigo-600 text-white py-4 rounded-2xl font-black shadow-lg">{{ isSavingContact ? '...' : 'Simpan' }}</button>
                </form>
             </div>
             <div class="md:col-span-2 bg-white p-8 rounded-[2.5rem] border border-slate-100 shadow-sm min-h-[500px]">
                <h2 class="text-xl font-black mb-6 text-slate-800">Daftar Kontak ({{ contacts.length }})</h2>
                <div v-if="contacts.length > 0" class="space-y-3 max-h-[500px] overflow-y-auto custom-scrollbar">
                    <div v-for="contact in contacts" :key="contact.id" class="flex justify-between items-center p-4 bg-slate-50 rounded-2xl border border-transparent hover:bg-white hover:border-indigo-100 transition-all group">
                        <div class="flex items-center gap-4">
                            <div class="w-10 h-10 rounded-full flex items-center justify-center font-black text-white" :class="contact.category === 'VIP' ? 'bg-amber-400' : contact.category === 'BLACKLIST' ? 'bg-slate-800' : 'bg-indigo-400'">{{ contact.name.substring(0,1) }}</div>
                            <div><h3 class="font-bold text-slate-800 text-sm">{{ contact.name }}</h3><p class="text-[11px] text-slate-400 font-medium">{{ contact.email }}</p></div>
                        </div>
                        <div class="flex items-center gap-4">
                             <div class="text-right"><span class="text-[10px] font-black px-2 py-1 rounded-md uppercase bg-indigo-50 text-indigo-600">{{ contact.category }}</span><div class="text-[10px] font-bold text-slate-400 mt-1">Bonus: {{ contact.priorityScore }}</div></div>
                             <button @click="handleDeleteContact(contact.id!)" class="text-slate-300 hover:text-red-500">✕</button>
                        </div>
                    </div>
                </div>
                <div v-else class="text-center py-20 text-slate-300"><div class="text-4xl mb-2 grayscale opacity-50">📇</div><p class="text-sm font-bold">Belum ada kontak.</p></div>
             </div>
        </div>

        <div v-if="activeTab === 'logs'" class="animate-in max-w-5xl mx-auto">
             <div class="bg-white p-8 rounded-[2.5rem] border border-slate-100 shadow-sm min-h-[500px]">
                <div class="flex justify-between items-center mb-6">
                    <h2 class="text-xl font-black text-slate-800">🧠 Memori Asisten</h2>
                    <button @click="loadLogs" class="text-xs font-bold text-indigo-600 hover:text-indigo-800">Refresh</button>
                </div>
                
                <div class="overflow-hidden rounded-2xl border border-slate-100">
                    <table class="w-full text-left text-sm text-slate-600">
                        <thead class="bg-slate-50 text-xs uppercase font-black text-slate-400">
                            <tr>
                                <th class="px-6 py-4">Waktu</th>
                                <th class="px-6 py-4">Action</th>
                                <th class="px-6 py-4">Tamu</th>
                                <th class="px-6 py-4">Alasan / Detail</th>
                                <th class="px-6 py-4 text-center">Skor</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-slate-100">
                            <tr v-for="log in memoryLogs" :key="log.id" class="hover:bg-slate-50/50 transition-colors">
                                <td class="px-6 py-4 font-mono text-xs">{{ new Date(log.timestamp).toLocaleString() }}</td>
                                <td class="px-6 py-4">
                                    <span v-if="log.action === 'AUTO_ACCEPTED'" class="bg-emerald-100 text-emerald-700 px-2 py-1 rounded-md text-[10px] font-black uppercase">Auto Accept</span>
                                    <span v-else-if="log.action === 'APPROVED'" class="bg-blue-100 text-blue-700 px-2 py-1 rounded-md text-[10px] font-black uppercase">Manual Approve</span>
                                    <span v-else-if="log.action === 'REJECTED'" class="bg-red-100 text-red-700 px-2 py-1 rounded-md text-[10px] font-black uppercase">Rejected</span>
                                    <span v-else class="bg-slate-100 text-slate-700 px-2 py-1 rounded-md text-[10px] font-black uppercase">{{ log.action }}</span>
                                </td>
                                <td class="px-6 py-4 font-bold text-slate-800">{{ log.guestName }}</td>
                                <td class="px-6 py-4 text-xs">{{ log.reason || '-' }}</td>
                                <td class="px-6 py-4 text-center font-mono font-bold" :class="log.scoreSnapshot >= 85 ? 'text-emerald-600' : 'text-slate-400'">
                                    {{ log.scoreSnapshot }}
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
             </div>
        </div>

      </div>
    </div>

    <div v-else class="max-w-lg mx-auto min-h-screen flex flex-col justify-center p-6">
      
      <div v-if="isBookingSuccess" class="bg-white rounded-[3rem] p-12 shadow-2xl shadow-emerald-100 border border-emerald-50 text-center animate-in">
          <div class="inline-block p-6 bg-emerald-50 rounded-full mb-6 text-6xl">✅</div>
          <h1 class="text-3xl font-black text-slate-800 mb-4">Permintaan Terkirim!</h1>
          <p class="text-slate-500 mb-8 leading-relaxed">
            Terima kasih telah menjadwalkan pertemuan.<br>
            Notifikasi konfirmasi akan dikirim ke email Anda.
          </p>
          <div class="p-4 bg-slate-50 rounded-2xl text-xs font-bold text-slate-400 uppercase tracking-widest">
            Tautan ini sekarang sudah hangus.
          </div>
      </div>

      <div v-else-if="isTokenExpired" class="bg-white rounded-[3rem] p-12 shadow-2xl shadow-red-100 border border-red-50 text-center animate-in">
          <div class="inline-block p-6 bg-red-50 rounded-full mb-6 text-6xl">🚫</div>
          <h1 class="text-3xl font-black text-slate-800 mb-4">Link Tidak Aktif</h1>
          <p class="text-slate-500 mb-8 leading-relaxed">
            Tautan ini sudah digunakan atau telah kadaluarsa.<br>
            Silakan hubungi pemilik untuk meminta tautan baru.
          </p>
          <div class="p-4 bg-slate-50 rounded-2xl text-xs font-bold text-slate-400 uppercase tracking-widest">
            Sesi Berakhir
          </div>
      </div>

      <div v-else class="bg-white rounded-[3rem] p-10 md:p-12 shadow-2xl shadow-indigo-100 border border-slate-50 animate-in">
        <div class="text-center mb-10">
          <div class="inline-block p-4 bg-indigo-50 rounded-3xl mb-4 text-4xl">👋</div>
          <h1 class="text-3xl font-black mb-2 tracking-tight text-slate-800">Atur Pertemuan</h1>
          <p class="text-slate-400 font-medium text-sm">Pilih tanggal untuk melihat ketersediaan waktu.</p>
        </div>
        
        <div class="space-y-4">
          <div class="relative">
            <input type="date" v-model="selectedDate" class="w-full p-5 bg-slate-50 rounded-3xl font-bold outline-none ring-2 ring-transparent focus:ring-indigo-500 focus:bg-white transition-all cursor-pointer text-slate-700" />
            <span class="absolute right-6 top-1/2 -translate-y-1/2 text-xl pointer-events-none">📅</span>
          </div>
          <button @click="handleCheckGuest" :disabled="loading" class="w-full bg-indigo-600 text-white py-5 rounded-3xl font-black shadow-xl shadow-indigo-200 hover:bg-indigo-700 hover:-translate-y-1 transition-all disabled:opacity-50 disabled:translate-y-0 disabled:shadow-none">{{ loading ? 'Mencari Slot...' : 'Cek Ketersediaan' }}</button>
        </div>

        <div v-if="errorMsg" class="mt-6 text-center text-red-500 font-bold p-4 bg-red-50 rounded-2xl text-xs border border-red-100">{{ errorMsg }}</div>
        
        <div class="grid grid-cols-1 gap-3 mt-8 max-h-[300px] overflow-y-auto pr-2 custom-scrollbar">
          <div v-if="availableSlots.length > 0" class="grid grid-cols-2 gap-3">
            <button v-for="slot in availableSlots" :key="slot.startTime" @click="openBooking(slot.label)" :class="[slot.isRecommended ? 'bg-indigo-50 border-indigo-200 text-indigo-700 ring-1 ring-indigo-200 hover:bg-indigo-100' : 'bg-white border-slate-200 text-slate-600 hover:border-indigo-400 hover:text-indigo-600']" class="relative p-4 border rounded-2xl text-left transition-all active:scale-95 shadow-sm group">
              <div v-if="slot.isRecommended" class="absolute -top-2 -right-2 bg-amber-400 text-white text-[9px] font-black px-2 py-0.5 rounded-full shadow-sm flex items-center gap-1"><span>★</span> BEST</div>
              <div class="font-black text-sm">{{ slot.label }}</div>
              <div class="text-[10px] mt-1 font-medium opacity-70 flex items-center gap-1"><span v-if="slot.isRecommended">⚡ Instant Book</span><span v-else>☕ Request</span></div>
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <div v-if="showModal" class="fixed inset-0 bg-slate-900/40 backdrop-blur-md flex items-center justify-center p-6 z-[999]"><div class="bg-white w-full max-w-sm rounded-[3rem] p-8 md:p-10 shadow-2xl animate-in scale-100 border border-white/20"><h2 class="text-2xl font-black mb-8 text-slate-800">Detail Anda</h2><form @submit.prevent="handleBooking" class="space-y-4"><input v-model="form.requesterName" placeholder="Nama Lengkap" required class="w-full p-4 bg-slate-50 rounded-2xl font-bold outline-none focus:ring-2 ring-indigo-500" /><input v-model="form.requesterEmail" type="email" placeholder="Email Aktif" required class="w-full p-4 bg-slate-50 rounded-2xl font-bold outline-none focus:ring-2 ring-indigo-500" /><input v-model="form.title" placeholder="Agenda Pertemuan" required class="w-full p-4 bg-slate-50 rounded-2xl font-bold outline-none focus:ring-2 ring-indigo-500" /><div class="flex gap-3 pt-6"><button @click="showModal = false" type="button" class="flex-1 py-4 font-bold text-slate-400 hover:bg-slate-50 rounded-2xl transition-colors">Batal</button><button type="submit" :disabled="submitting" class="flex-1 bg-indigo-600 text-white py-4 rounded-2xl font-black shadow-lg shadow-indigo-100 hover:bg-indigo-700">{{ submitting ? '...' : 'Booking' }}</button></div></form></div></div>
    <div v-if="showTaskModal" class="fixed inset-0 bg-slate-900/40 backdrop-blur-md flex items-center justify-center p-6 z-[999]"><div class="bg-white w-full max-w-sm rounded-[3rem] p-10 shadow-2xl animate-in"><h2 class="text-2xl font-black mb-6 text-slate-800 flex items-center gap-2"><span class="text-indigo-500 text-3xl">⚡</span> Quick Task</h2><form @submit.prevent="handleQuickTask" class="space-y-4"><input v-model="taskForm.title" placeholder="Nama Agenda" required class="w-full p-4 bg-slate-50 rounded-2xl font-bold outline-none focus:ring-2 ring-indigo-500" /><div class="grid grid-cols-2 gap-3"><div><label class="text-[10px] font-black text-slate-400 ml-2 uppercase">Mulai</label><input type="datetime-local" v-model="taskForm.startTime" required class="w-full p-3 bg-slate-50 rounded-2xl font-bold outline-none text-xs" /></div><div><label class="text-[10px] font-black text-slate-400 ml-2 uppercase">Selesai</label><input type="datetime-local" v-model="taskForm.endTime" required class="w-full p-3 bg-slate-50 rounded-2xl font-bold outline-none text-xs" /></div></div><button type="submit" :disabled="submitting" class="w-full bg-indigo-600 text-white py-4 rounded-2xl font-black mt-4 shadow-lg shadow-indigo-100 hover:bg-indigo-700">{{ submitting ? 'Syncing...' : 'Simpan ke Google' }}</button><button @click="showTaskModal = false" type="button" class="w-full text-slate-400 font-bold text-sm mt-2">Batal</button></form></div></div>
    <div v-if="showRoutineModal" class="fixed inset-0 bg-slate-900/40 backdrop-blur-md flex items-center justify-center p-6 z-[999]"><div class="bg-white w-full max-w-sm rounded-[3rem] p-10 shadow-2xl animate-in"><h2 class="text-2xl font-black mb-8 text-slate-800">Rutinitas Baru</h2><form @submit.prevent="handleAddRoutine" class="space-y-4"><input v-model="routineForm.name" placeholder="Nama (e.g. Istirahat)" required class="w-full p-4 bg-slate-50 rounded-2xl font-bold outline-none focus:ring-2 ring-indigo-500" /><div class="flex gap-4"><input type="time" v-model="routineForm.startTime" required class="w-full p-4 bg-slate-50 rounded-2xl font-bold outline-none" /><input type="time" v-model="routineForm.endTime" required class="w-full p-4 bg-slate-50 rounded-2xl font-bold outline-none" /></div><button type="submit" class="w-full bg-indigo-600 text-white py-4 rounded-2xl font-black mt-4 shadow-lg shadow-indigo-100 hover:bg-indigo-700">Simpan</button><button @click="showRoutineModal = false" type="button" class="w-full text-slate-400 font-bold mt-2">Batal</button></form></div></div>
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