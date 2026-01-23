<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import schedulerApi, { type UserContact } from '../services/schedulerApi'
import ApprovalInbox from './ApprovalInbox.vue'

// State Owner
const activeTab = ref('dashboard')
const ownerBookings = ref<any[]>([]) 
const syncStatus = ref<any>(null)
const generatedLink = ref<any>(null)
const workSchedules = ref<any[]>([])
const memoryLogs = ref<any[]>([])
const contacts = ref<UserContact[]>([])
const autoAcceptThreshold = ref(85)
const isSavingConfig = ref(false)
const isSavingContact = ref(false)
const contactForm = ref<UserContact>({ email: '', name: '', category: 'VIP', priorityScore: 50 })
const showTaskModal = ref(false)
const showRoutineModal = ref(false)
const submitting = ref(false)
const taskForm = ref({ title: '', description: 'Quick task', requesterName: 'Owner', requesterEmail: '', startTime: '', endTime: '' })
const routineForm = ref({ name: '', startTime: '', endTime: '' })

// Calendar Logic
const today = new Date()
const currentMonth = ref(today.getMonth())
const currentYear = ref(today.getFullYear())
const selectedDashboardDate = ref(today.toISOString().split('T')[0]) 
const weekDays = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const monthNames = ["Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember"]

let pollingInterval: any = null

onMounted(async () => {
    await loadAllData()
    pollingInterval = setInterval(async () => {
       try { ownerBookings.value = await schedulerApi.getAppointments() } catch (e) { console.error("Sync fail", e) }
    }, 5000)
})
onUnmounted(() => { if (pollingInterval) clearInterval(pollingInterval) })

const loadAllData = async () => {
    try {
        ownerBookings.value = await schedulerApi.getAppointments()
        syncStatus.value = await schedulerApi.getDashboardStatus()
        taskForm.value.requesterEmail = syncStatus.value?.email || ''
        workSchedules.value = (await schedulerApi.getWorkSchedules()) || []
        weekDays.forEach(day => { if(!workSchedules.value.find((s: any) => s.dayOfWeek === day)) workSchedules.value.push({ dayOfWeek: day, workingDay: false, startTime: '09:00:00', endTime: '17:00:00' }) })
        
        const configs = await schedulerApi.getAllConfigs(); 
        const th = configs.find((c: any) => c.configKey === 'AUTO_ACCEPT_THRESHOLD'); 
        if (th) autoAcceptThreshold.value = parseInt(th.configValue)
        
        contacts.value = await schedulerApi.getContacts()
        memoryLogs.value = await schedulerApi.getDecisionLogs()
    } catch(e) { console.error("Init Error", e) }
}

// Computed
const calendarGrid = computed(() => { const firstDay = new Date(currentYear.value, currentMonth.value, 1).getDay(); const daysInMonth = new Date(currentYear.value, currentMonth.value + 1, 0).getDate(); const padding = firstDay === 0 ? 6 : firstDay - 1; const days = []; for (let i = 0; i < padding; i++) days.push(null); for (let i = 1; i <= daysInMonth; i++) { const dateStr = `${currentYear.value}-${String(currentMonth.value + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`; days.push({ day: i, date: dateStr }) } return days })
const nextMonth = () => { if (currentMonth.value === 11) { currentMonth.value = 0; currentYear.value++ } else { currentMonth.value++ } }
const prevMonth = () => { if (currentMonth.value === 0) { currentMonth.value = 11; currentYear.value-- } else { currentMonth.value-- } }
const hasAppointment = (dateStr: string) => { return ownerBookings.value.some(b => b.startTime && b.startTime.startsWith(dateStr) && b.status !== 'REJECTED') }
const selectedDayAgenda = computed(() => { return ownerBookings.value.filter(b => b.startTime && b.startTime.startsWith(selectedDashboardDate.value)).sort((a, b) => a.startTime.localeCompare(b.startTime)) })

// Actions
const handleApprove = async (id: string) => { if(!confirm("Setujui?")) return; await schedulerApi.approveAppointment(id); await loadAllData() }
const handleReject = async (id: string) => { if(!confirm("Tolak?")) return; await schedulerApi.rejectAppointment(id); await loadAllData() }
const handleQuickTask = async () => { submitting.value = true; try { await schedulerApi.createQuickTask(taskForm.value); showTaskModal.value = false; await loadAllData() } catch (e) { alert("Gagal") } finally { submitting.value = false } }
const handleUpdateWorkDay = async (day: string) => { const data = workSchedules.value.find(s => s.dayOfWeek === day); if(data) await schedulerApi.updateWorkSchedule(data) }
const saveBrainConfig = async () => { isSavingConfig.value = true; try { await schedulerApi.updateConfig('AUTO_ACCEPT_THRESHOLD', autoAcceptThreshold.value.toString()); alert("Saved!") } catch(e){ alert("Gagal") } finally { isSavingConfig.value = false } }
const handleAddContact = async () => { isSavingContact.value = true; try { await schedulerApi.addContact(contactForm.value); await loadAllData(); contactForm.value = { email: '', name: '', category: 'VIP', priorityScore: 50 }; alert("Kontak tersimpan") } catch(e){alert("Gagal")} finally { isSavingContact.value = false } }
const handleDeleteContact = async (id: string) => { if(confirm("Hapus?")) { await schedulerApi.deleteContact(id); await loadAllData() } }
const handleAddRoutine = async () => { try { await schedulerApi.addPersonalBlock({ ...routineForm.value, startTime: routineForm.value.startTime + ":00", endTime: routineForm.value.endTime + ":00" }); showRoutineModal.value = false; await loadAllData() } catch(e){alert("Gagal")} }
const handleDeleteRoutine = async (id: number) => { if(confirm("Hapus?")) { await schedulerApi.deletePersonalBlock(id); await loadAllData() } }
const handleGenerateLink = async (type: 'WORK' | 'SOCIAL') => { generatedLink.value = await schedulerApi.generateShareLink(type) }
const copyToClipboard = (text: string) => navigator.clipboard.writeText(text).then(() => alert("Copied!"))
const loadLogs = async () => { memoryLogs.value = await schedulerApi.getDecisionLogs() }
</script>

<template>
  <div class="max-w-[1400px] mx-auto p-6 md:p-10">
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
        <div class="lg:col-span-7 space-y-6">
            <ApprovalInbox :bookings="ownerBookings" @approve="handleApprove" @reject="handleReject" />
            
            <div class="bg-white rounded-[2rem] p-6 border border-slate-100 shadow-sm h-fit">
                <div class="flex justify-between items-end mb-6"><div><h2 class="text-2xl font-black text-slate-800">{{ monthNames[currentMonth] }}</h2><p class="text-slate-400 font-bold text-sm">{{ currentYear }}</p></div><div class="flex gap-2"><button @click="prevMonth" class="w-8 h-8 flex items-center justify-center rounded-full border border-slate-100 hover:bg-slate-50">←</button><button @click="nextMonth" class="w-8 h-8 flex items-center justify-center rounded-full border border-slate-100 hover:bg-slate-50">→</button></div></div>
                <div class="grid grid-cols-7 gap-2 text-center mb-4"><div v-for="d in ['Sn', 'Sl', 'Rb', 'Km', 'Jm', 'Sb', 'Mg']" :key="d" class="text-[10px] font-black text-slate-300 uppercase tracking-widest">{{ d }}</div></div>
                <div class="grid grid-cols-7 gap-1.5"><div v-for="(day, idx) in calendarGrid" :key="idx" class="aspect-square"><div v-if="day" @click="selectedDashboardDate = day.date" :class="[selectedDashboardDate === day.date ? 'bg-indigo-600 text-white shadow-md' : 'hover:bg-indigo-50 text-slate-600', hasAppointment(day.date) ? 'font-bold' : 'font-medium']" class="w-full h-full rounded-2xl flex flex-col items-center justify-center cursor-pointer transition-all relative group"><span class="text-xs z-10">{{ day.day }}</span><div v-if="hasAppointment(day.date)" :class="ownerBookings.some(b => b.startTime.startsWith(day.date) && b.status === 'PENDING') ? 'bg-amber-400' : (selectedDashboardDate === day.date ? 'bg-white' : 'bg-indigo-400')" class="w-1.5 h-1.5 rounded-full mt-1"></div></div></div></div>
            </div>
        </div>
        
        <div class="lg:col-span-5 bg-white rounded-[2rem] p-6 border border-slate-100 shadow-sm h-fit min-h-[500px] flex flex-col">
            <div class="flex justify-between items-center mb-6"><div><h2 class="text-lg font-black text-slate-800">Agenda Harian</h2><p class="text-[10px] font-bold text-slate-400 mt-0.5 uppercase tracking-widest">📅 {{ selectedDashboardDate }}</p></div><button @click="showTaskModal = true" class="bg-indigo-600 text-white text-[10px] font-black uppercase tracking-wider px-3 py-2 rounded-xl shadow-lg hover:bg-indigo-700 flex items-center gap-1">+ Task</button></div>
            <div class="flex-1 overflow-y-auto pr-2 custom-scrollbar">
                <div v-if="selectedDayAgenda.length > 0" class="space-y-4">
                    <div v-for="agenda in selectedDayAgenda" :key="agenda.id" class="bg-white rounded-2xl p-4 border border-slate-100 shadow-sm hover:border-indigo-100 transition-all">
                        <div class="flex justify-between items-start mb-2"><span class="text-[10px] font-black px-2 py-1 rounded-md bg-indigo-50 text-indigo-600">{{ agenda.startTime.split('T')[1].substring(0,5) }} - {{ agenda.endTime.split('T')[1].substring(0,5) }}</span>
                        <div v-if="agenda.status === 'GOOGLE'"><img src="https://www.svgrepo.com/show/355037/google-icon.svg" class="w-3.5 h-3.5 opacity-70"></div><div v-else-if="agenda.status === 'ACCEPTED'"><span class="text-emerald-500 text-[10px] font-bold">● Synced</span></div><div v-else-if="agenda.status === 'PENDING'"><span class="text-amber-500 text-[10px] font-bold">● Menunggu</span></div><div v-else-if="agenda.status === 'REJECTED'"><span class="text-red-400 text-[10px] font-bold line-through">Ditolak</span></div></div>
                        <h3 class="font-bold text-slate-800 text-sm leading-snug mb-1">{{ agenda.title }}</h3><p v-if="agenda.status !== 'GOOGLE'" class="text-[10px] text-slate-400 font-medium">Oleh: {{ agenda.requesterName }}</p>
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

    <div v-if="activeTab === 'settings'" class="animate-in max-w-2xl mx-auto"><div class="bg-white p-10 rounded-[3.5rem] border border-slate-100 shadow-lg"><h2 class="text-3xl font-black mb-6 text-slate-800">🧠 Pengaturan Otak</h2><div class="bg-slate-50 p-8 rounded-[2rem] mb-8"><div class="flex justify-between items-end mb-4"><label class="font-bold text-slate-700">Auto Accept Threshold</label><span class="text-3xl font-black text-indigo-600">{{ autoAcceptThreshold }}</span></div><input type="range" min="0" max="100" v-model="autoAcceptThreshold" class="w-full h-4 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-indigo-600"><div class="flex justify-between mt-3 text-[10px] font-black uppercase text-slate-400"><span>Terima Semua (0)</span><span>Eksklusif (100)</span></div></div><button @click="saveBrainConfig" :disabled="isSavingConfig" class="w-full bg-slate-900 text-white px-10 py-4 rounded-2xl font-black shadow-xl hover:bg-indigo-600">{{ isSavingConfig ? '...' : 'Simpan Pengaturan' }}</button></div></div>

    <div v-if="activeTab === 'contacts'" class="animate-in max-w-4xl mx-auto grid grid-cols-1 md:grid-cols-3 gap-8"><div class="md:col-span-1 bg-white p-8 rounded-[2.5rem] border border-slate-100 shadow-sm h-fit"><h2 class="text-xl font-black mb-6 text-slate-800">Tambah Kontak</h2><form @submit.prevent="handleAddContact" class="space-y-4"><input v-model="contactForm.email" type="email" placeholder="Email" required class="w-full p-4 bg-slate-50 rounded-2xl font-bold text-sm outline-none" /><input v-model="contactForm.name" placeholder="Nama Lengkap" required class="w-full p-4 bg-slate-50 rounded-2xl font-bold text-sm outline-none" /><div><label class="text-[10px] font-black text-slate-400 ml-2 uppercase">Kategori</label><select v-model="contactForm.category" class="w-full p-4 bg-slate-50 rounded-2xl font-bold text-sm outline-none"><option value="VIP">VIP (Prioritas)</option><option value="TEAM">Team</option><option value="BLACKLIST">Blacklist</option></select></div><div><label class="text-[10px] font-black text-slate-400 ml-2 uppercase">Bonus Skor</label><input type="number" v-model="contactForm.priorityScore" class="w-full p-4 bg-slate-50 rounded-2xl font-bold text-sm outline-none" /></div><button type="submit" :disabled="isSavingContact" class="w-full bg-indigo-600 text-white py-4 rounded-2xl font-black shadow-lg">{{ isSavingContact ? '...' : 'Simpan' }}</button></form></div><div class="md:col-span-2 bg-white p-8 rounded-[2.5rem] border border-slate-100 shadow-sm min-h-[500px]"><h2 class="text-xl font-black mb-6 text-slate-800">Daftar Kontak ({{ contacts.length }})</h2><div v-if="contacts.length > 0" class="space-y-3 max-h-[500px] overflow-y-auto custom-scrollbar"><div v-for="contact in contacts" :key="contact.id" class="flex justify-between items-center p-4 bg-slate-50 rounded-2xl border border-transparent hover:bg-white hover:border-indigo-100 transition-all group"><div class="flex items-center gap-4"><div class="w-10 h-10 rounded-full flex items-center justify-center font-black text-white" :class="contact.category === 'VIP' ? 'bg-amber-400' : contact.category === 'BLACKLIST' ? 'bg-slate-800' : 'bg-indigo-400'">{{ contact.name.substring(0,1) }}</div><div><h3 class="font-bold text-slate-800 text-sm">{{ contact.name }}</h3><p class="text-[11px] text-slate-400 font-medium">{{ contact.email }}</p></div></div><div class="flex items-center gap-4"><div class="text-right"><span class="text-[10px] font-black px-2 py-1 rounded-md uppercase bg-indigo-50 text-indigo-600">{{ contact.category }}</span><div class="text-[10px] font-bold text-slate-400 mt-1">Bonus: {{ contact.priorityScore }}</div></div><button @click="handleDeleteContact(contact.id!)" class="text-slate-300 hover:text-red-500">✕</button></div></div></div><div v-else class="text-center py-20 text-slate-300"><div class="text-4xl mb-2 grayscale opacity-50">📇</div><p class="text-sm font-bold">Belum ada kontak.</p></div></div></div>

    <div v-if="activeTab === 'logs'" class="animate-in max-w-5xl mx-auto"><div class="bg-white p-8 rounded-[2.5rem] border border-slate-100 shadow-sm min-h-[500px]"><div class="flex justify-between items-center mb-6"><h2 class="text-xl font-black text-slate-800">🧠 Memori Asisten</h2><button @click="loadLogs" class="text-xs font-bold text-indigo-600 hover:text-indigo-800">Refresh</button></div><div class="overflow-hidden rounded-2xl border border-slate-100"><table class="w-full text-left text-sm text-slate-600"><thead class="bg-slate-50 text-xs uppercase font-black text-slate-400"><tr><th class="px-6 py-4">Waktu</th><th class="px-6 py-4">Action</th><th class="px-6 py-4">Tamu</th><th class="px-6 py-4">Alasan / Detail</th><th class="px-6 py-4 text-center">Skor</th></tr></thead><tbody class="divide-y divide-slate-100"><tr v-for="log in memoryLogs" :key="log.id" class="hover:bg-slate-50/50 transition-colors"><td class="px-6 py-4 font-mono text-xs">{{ new Date(log.timestamp).toLocaleString() }}</td><td class="px-6 py-4"><span v-if="log.action === 'AUTO_ACCEPTED'" class="bg-emerald-100 text-emerald-700 px-2 py-1 rounded-md text-[10px] font-black uppercase">Auto Accept</span><span v-else-if="log.action === 'APPROVED'" class="bg-blue-100 text-blue-700 px-2 py-1 rounded-md text-[10px] font-black uppercase">Manual Approve</span><span v-else-if="log.action === 'REJECTED'" class="bg-red-100 text-red-700 px-2 py-1 rounded-md text-[10px] font-black uppercase">Rejected</span><span v-else class="bg-slate-100 text-slate-700 px-2 py-1 rounded-md text-[10px] font-black uppercase">{{ log.action }}</span></td><td class="px-6 py-4 font-bold text-slate-800">{{ log.guestName }}</td><td class="px-6 py-4 text-xs">{{ log.reason || '-' }}</td><td class="px-6 py-4 text-center font-mono font-bold" :class="log.scoreSnapshot >= 85 ? 'text-emerald-600' : 'text-slate-400'">{{ log.scoreSnapshot }}</td></tr></tbody></table></div></div></div>

    <div v-if="showTaskModal" class="fixed inset-0 bg-slate-900/40 backdrop-blur-md flex items-center justify-center p-6 z-[999]"><div class="bg-white w-full max-w-sm rounded-[3rem] p-10 shadow-2xl animate-in"><h2 class="text-2xl font-black mb-6 text-slate-800 flex items-center gap-2"><span class="text-indigo-500 text-3xl">⚡</span> Quick Task</h2><form @submit.prevent="handleQuickTask" class="space-y-4"><input v-model="taskForm.title" placeholder="Nama Agenda" required class="w-full p-4 bg-slate-50 rounded-2xl font-bold outline-none focus:ring-2 ring-indigo-500" /><div class="grid grid-cols-2 gap-3"><div><label class="text-[10px] font-black text-slate-400 ml-2 uppercase">Mulai</label><input type="datetime-local" v-model="taskForm.startTime" required class="w-full p-3 bg-slate-50 rounded-2xl font-bold outline-none text-xs" /></div><div><label class="text-[10px] font-black text-slate-400 ml-2 uppercase">Selesai</label><input type="datetime-local" v-model="taskForm.endTime" required class="w-full p-3 bg-slate-50 rounded-2xl font-bold outline-none text-xs" /></div></div><button type="submit" :disabled="submitting" class="w-full bg-indigo-600 text-white py-4 rounded-2xl font-black mt-4 shadow-lg shadow-indigo-100 hover:bg-indigo-700">{{ submitting ? 'Syncing...' : 'Simpan ke Google' }}</button><button @click="showTaskModal = false" type="button" class="w-full text-slate-400 font-bold text-sm mt-2">Batal</button></form></div></div>
    <div v-if="showRoutineModal" class="fixed inset-0 bg-slate-900/40 backdrop-blur-md flex items-center justify-center p-6 z-[999]"><div class="bg-white w-full max-w-sm rounded-[3rem] p-10 shadow-2xl animate-in"><h2 class="text-2xl font-black mb-8 text-slate-800">Rutinitas Baru</h2><form @submit.prevent="handleAddRoutine" class="space-y-4"><input v-model="routineForm.name" placeholder="Nama (e.g. Istirahat)" required class="w-full p-4 bg-slate-50 rounded-2xl font-bold outline-none focus:ring-2 ring-indigo-500" /><div class="flex gap-4"><input type="time" v-model="routineForm.startTime" required class="w-full p-4 bg-slate-50 rounded-2xl font-bold outline-none" /><input type="time" v-model="routineForm.endTime" required class="w-full p-4 bg-slate-50 rounded-2xl font-bold outline-none" /></div><button type="submit" class="w-full bg-indigo-600 text-white py-4 rounded-2xl font-black mt-4 shadow-lg shadow-indigo-100 hover:bg-indigo-700">Simpan</button><button @click="showRoutineModal = false" type="button" class="w-full text-slate-400 font-bold mt-2">Batal</button></form></div></div>
  </div>
</template>