import axios from 'axios'

// PERBAIKAN: Gunakan Domain Production (HTTPS), bukan localhost lagi
// Nginx akan mengurus proxy-nya, tapi kita tembak langsung ke domain biar aman
const API_URL = 'https://aawzyy.my.id/api'

const apiClient = axios.create({
  baseURL: API_URL,
  withCredentials: true 
})

// --- INTERFACES ---
export interface TimeSlot {
  label: string       
  startTime: string   
  score: number       
  isRecommended: boolean 
}

export interface UserContact {
  id?: string
  email: string
  name: string
  category: string
  priorityScore: number
}

export default {
  // --- DASHBOARD & APPOINTMENTS ---
  async getAppointments() {
    const response = await apiClient.get('/dashboard/combined')
    return response.data
  },

  async getDashboardStatus() {
    const response = await apiClient.get('/dashboard/status')
    return response.data
  },

  async approveAppointment(id: string) {
    return apiClient.post(`/appointments/${id}/approve`)
  },

  async rejectAppointment(id: string) {
    return apiClient.post(`/appointments/${id}/reject`)
  },

  async createAppointment(appointmentData: any) {
    return apiClient.post('/appointments', appointmentData)
  },

  async createQuickTask(payload: any) {
    return apiClient.post('/appointments/tasks/quick', payload)
  },

  // --- SCHEDULES & BLOCKS ---
  async getWorkSchedules() {
    const response = await apiClient.get('/schedules')
    return response.data
  },

  async updateWorkSchedule(payload: any) {
    const response = await apiClient.post('/schedules', payload)
    return response.data
  },

  async addPersonalBlock(payload: any) {
    return apiClient.post('/personal-blocks', payload)
  },

  async deletePersonalBlock(id: number) {
    return apiClient.delete(`/personal-blocks/${id}`)
  },

  // --- SHARE & GUEST ---
  async generateShareLink(type: 'WORK' | 'SOCIAL') {
    const response = await apiClient.post(`/share?type=${type}`)
    return response.data
  },

  async checkGuestAvailability(token: string, date: string): Promise<TimeSlot[]> {
    const response = await apiClient.get(`/availability/token/${token}?date=${date}`)
    return response.data
  },

  // --- CONFIGS (SETTINGS) ---
  async getAllConfigs() {
    const response = await apiClient.get('/configs')
    return response.data
  },

  async updateConfig(key: string, value: string) {
    return apiClient.post('/configs', { key, value })
  },

  // --- CONTACTS (VIP) ---
  async getContacts() {
    const response = await apiClient.get('/contacts')
    return response.data
  },

  async addContact(contact: UserContact) {
    return apiClient.post('/contacts', contact)
  },

  async deleteContact(id: string) {
    return apiClient.delete(`/contacts/${id}`)
  }
}