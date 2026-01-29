import 'package:flutter/material.dart';
import 'package:flutter_mobx/flutter_mobx.dart';
import 'package:intl/intl.dart';
import '../../../../core/services/injection_container.dart';
import '../../domain/entities/appointment.dart';
import '../stores/appointment_store.dart';
import '../widgets/inbox_widget.dart';

class DashboardPage extends StatefulWidget {
  const DashboardPage({super.key});

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends State<DashboardPage> {
  // Ambil Store dari Injection
  final AppointmentStore _store = sl<AppointmentStore>();

  // State Navigasi Tab
  String activeTab = 'dashboard';

  // State Kalender
  DateTime _focusedDate = DateTime.now(); // Bulan yang sedang dilihat
  DateTime _selectedDate = DateTime.now(); // Tanggal yang diklik (Highlight)

  @override
  void initState() {
    super.initState();
    // Reset tanggal terpilih ke hari ini saat buka dashboard
    _selectedDate = DateTime.now();
    _store.loadAppointments();
  }

  // --- LOGIC: Filter Agenda Berdasarkan Tanggal Terpilih ---
  List<Appointment> get selectedDateAgenda {
    // Format tanggal terpilih menjadi string 'yyyy-MM-dd' untuk pencocokan
    final dateStr = DateFormat('yyyy-MM-dd').format(_selectedDate);

    return _store.appointments
        .where(
          (apt) =>
              // Cocokkan bagian awal string waktu (misal: "2023-10-27T...")
              apt.startTime.toString().startsWith(dateStr) &&
              apt.status != 'REJECTED',
        ) // Jangan tampilkan yang ditolak
        .toList()
      // Urutkan berdasarkan jam
      ..sort((a, b) => a.startTime.compareTo(b.startTime));
  }

  // --- LOGIC: Generate Grid Kalender ---
  List<DateTime?> _generateCalendarGrid() {
    final year = _focusedDate.year;
    final month = _focusedDate.month;

    // Cari hari pertama bulan ini
    final firstDayOfMonth = DateTime(year, month, 1);
    // Cari jumlah hari dalam bulan ini
    final daysInMonth = DateTime(year, month + 1, 0).day;

    // Hitung padding (kotak kosong sebelum tanggal 1)
    // Di Dart: Senin=1 ... Minggu=7
    // Kita ingin Senin di kolom pertama (index 0) -> jadi (weekday - 1)
    final firstWeekday = firstDayOfMonth.weekday;
    final padding = firstWeekday - 1;

    final List<DateTime?> days = [];

    // 1. Isi Padding Awal (Null)
    for (int i = 0; i < padding; i++) {
      days.add(null);
    }

    // 2. Isi Tanggal
    for (int i = 1; i <= daysInMonth; i++) {
      days.add(DateTime(year, month, i));
    }

    return days;
  }

  // Fungsi Ganti Bulan (Prev/Next)
  void _changeMonth(int offset) {
    setState(() {
      _focusedDate = DateTime(
        _focusedDate.year,
        _focusedDate.month + offset,
        1,
      );
    });
  }

  // --- UI: MEMBUKA MODAL QUICK TASK ---
  void _showQuickTaskModal(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => QuickTaskModal(
        // Memanggil Widget Modal Terpisah
        onSave: (title, start, end) async {
          // Panggil Logic Store di sini
          final success = await _store.createQuickTask(title, start, end);

          if (success && mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text("Agenda berhasil disimpan!")),
            );
          }
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF8FAFC), // bg-slate-50
      // Floating Action Button (Quick Task)
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _showQuickTaskModal(context),
        backgroundColor: const Color(0xFF4F46E5), // Indigo-600
        elevation: 4,
        icon: const Icon(Icons.add_task, color: Colors.white),
        label: const Text(
          "Quick Task",
          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white),
        ),
      ),

      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 1. HEADER
              _buildHeader(),
              const SizedBox(height: 24),

              // 2. TABS
              _buildTabs(),
              const SizedBox(height: 24),

              // 3. MAIN CONTENT (Reactive)
              Observer(
                builder: (_) {
                  if (_store.isLoading) {
                    return const SizedBox(
                      height: 300,
                      child: Center(child: CircularProgressIndicator()),
                    );
                  }

                  if (_store.errorMessage != null) {
                    return Center(
                      child: Padding(
                        padding: const EdgeInsets.all(20.0),
                        child: Text(
                          "Error: ${_store.errorMessage}",
                          style: const TextStyle(color: Colors.red),
                        ),
                      ),
                    );
                  }

                  return Column(
                    children: [
                      // A. INBOX WIDGET (Persetujuan)
                      InboxWidget(
                        appointments: _store.appointments,
                        store: _store,
                      ),

                      // B. KALENDER UTAMA
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(24),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(32),
                          border: Border.all(color: const Color(0xFFF1F5F9)),
                        ),
                        child: Column(
                          children: [
                            // Header Kalender (Bulan & Navigasi)
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      DateFormat('MMMM').format(_focusedDate),
                                      style: const TextStyle(
                                        fontSize: 24,
                                        fontWeight: FontWeight.w900,
                                        color: Color(0xFF1E293B), // Slate-800
                                      ),
                                    ),
                                    Text(
                                      DateFormat('yyyy').format(_focusedDate),
                                      style: const TextStyle(
                                        fontWeight: FontWeight.bold,
                                        color: Colors.grey,
                                      ),
                                    ),
                                  ],
                                ),
                                Row(
                                  children: [
                                    IconButton(
                                      icon: const Icon(Icons.chevron_left),
                                      onPressed: () => _changeMonth(-1),
                                      splashRadius: 20,
                                    ),
                                    IconButton(
                                      icon: const Icon(Icons.chevron_right),
                                      onPressed: () => _changeMonth(1),
                                      splashRadius: 20,
                                    ),
                                  ],
                                ),
                              ],
                            ),
                            const SizedBox(height: 20),

                            // Nama Hari
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children:
                                  ['Sn', 'Sl', 'Rb', 'Km', 'Jm', 'Sb', 'Mg']
                                      .map(
                                        (day) => Expanded(
                                          child: Center(
                                            child: Text(
                                              day,
                                              style: const TextStyle(
                                                fontSize: 10,
                                                fontWeight: FontWeight.w900,
                                                color: Colors.grey,
                                              ),
                                            ),
                                          ),
                                        ),
                                      )
                                      .toList(),
                            ),
                            const SizedBox(height: 10),

                            // GRID TANGGAL
                            GridView.builder(
                              shrinkWrap: true,
                              physics: const NeverScrollableScrollPhysics(),
                              gridDelegate:
                                  const SliverGridDelegateWithFixedCrossAxisCount(
                                    crossAxisCount: 7,
                                    childAspectRatio: 1,
                                  ),
                              itemCount: _generateCalendarGrid().length,
                              itemBuilder: (context, index) {
                                final date = _generateCalendarGrid()[index];

                                // Kalau null (padding), tampilkan kotak kosong
                                if (date == null) return const SizedBox();

                                final isSelected = DateUtils.isSameDay(
                                  date,
                                  _selectedDate,
                                );
                                final isToday = DateUtils.isSameDay(
                                  date,
                                  DateTime.now(),
                                );

                                // Cek apakah ada jadwal di tanggal ini
                                bool hasEvent = _store.appointments.any(
                                  (a) =>
                                      DateUtils.isSameDay(a.startTime, date) &&
                                      a.status != 'REJECTED',
                                );

                                bool hasPending = _store.appointments.any(
                                  (a) =>
                                      DateUtils.isSameDay(a.startTime, date) &&
                                      a.status == 'PENDING',
                                );

                                return InkWell(
                                  onTap: () =>
                                      setState(() => _selectedDate = date),
                                  borderRadius: BorderRadius.circular(12),
                                  child: Container(
                                    margin: const EdgeInsets.all(2),
                                    decoration: BoxDecoration(
                                      color: isSelected
                                          ? const Color(
                                              0xFF4F46E5,
                                            ) // Indigo (Selected)
                                          : Colors.transparent,
                                      borderRadius: BorderRadius.circular(12),
                                      border: isToday && !isSelected
                                          ? Border.all(
                                              color: const Color(0xFF4F46E5),
                                            )
                                          : null,
                                    ),
                                    child: Column(
                                      mainAxisAlignment:
                                          MainAxisAlignment.center,
                                      children: [
                                        Text(
                                          "${date.day}",
                                          style: TextStyle(
                                            fontSize: 12,
                                            fontWeight: isSelected || hasEvent
                                                ? FontWeight.bold
                                                : FontWeight.normal,
                                            color: isSelected
                                                ? Colors.white
                                                : (isToday
                                                      ? const Color(0xFF4F46E5)
                                                      : Colors.black87),
                                          ),
                                        ),
                                        // DOT Indikator
                                        if (hasEvent)
                                          Container(
                                            margin: const EdgeInsets.only(
                                              top: 4,
                                            ),
                                            width: 4,
                                            height: 4,
                                            decoration: BoxDecoration(
                                              color: isSelected
                                                  ? Colors.white
                                                  : (hasPending
                                                        ? Colors.amber
                                                        : const Color(
                                                            0xFF4F46E5,
                                                          )),
                                              shape: BoxShape.circle,
                                            ),
                                          ),
                                      ],
                                    ),
                                  ),
                                );
                              },
                            ),
                          ],
                        ),
                      ),

                      const SizedBox(height: 24),

                      // C. AGENDA HARIAN
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(24),
                        margin: const EdgeInsets.only(
                          bottom: 80,
                        ), // Biar gak ketutup FAB
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(32),
                          border: Border.all(color: const Color(0xFFF1F5F9)),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                const Text(
                                  "Agenda Harian",
                                  style: TextStyle(
                                    fontSize: 18,
                                    fontWeight: FontWeight.w900,
                                    color: Color(0xFF1E293B),
                                  ),
                                ),
                                Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 8,
                                    vertical: 4,
                                  ),
                                  decoration: BoxDecoration(
                                    color: Colors.grey[100],
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: Text(
                                    "📅 ${DateFormat('dd MMM').format(_selectedDate)}",
                                    style: const TextStyle(
                                      fontSize: 12,
                                      fontWeight: FontWeight.bold,
                                      color: Colors.grey,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 20),

                            if (selectedDateAgenda.isEmpty)
                              Center(
                                child: Padding(
                                  padding: const EdgeInsets.symmetric(
                                    vertical: 30,
                                  ),
                                  child: Column(
                                    children: [
                                      const Text(
                                        "☕",
                                        style: TextStyle(fontSize: 40),
                                      ),
                                      const SizedBox(height: 8),
                                      Text(
                                        "Tidak ada jadwal.",
                                        style: TextStyle(
                                          color: Colors.grey[400],
                                          fontWeight: FontWeight.bold,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              )
                            else
                              ...selectedDateAgenda
                                  .map((apt) => _buildAgendaItem(apt))
                                  .toList(),
                          ],
                        ),
                      ),
                    ],
                  );
                },
              ),
            ],
          ),
        ),
      ),
    );
  }

  // --- WIDGET PENDUKUNG ---

  Widget _buildHeader() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: const [
            Text(
              "Dashboard",
              style: TextStyle(
                fontSize: 28,
                fontWeight: FontWeight.w900,
                color: Color(0xFF1E293B),
                letterSpacing: -1,
              ),
            ),
            Text(
              "Selamat datang kembali!",
              style: TextStyle(
                color: Color(0xFF94A3B8), // Slate-400
                fontWeight: FontWeight.w500,
                fontSize: 14,
              ),
            ),
          ],
        ),
        // Badge Live Sync
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: const Color(0xFFF1F5F9)),
          ),
          child: Row(
            children: [
              Container(
                width: 8,
                height: 8,
                decoration: const BoxDecoration(
                  color: Color(0xFF34D399), // Emerald-400
                  shape: BoxShape.circle,
                ),
              ),
              const SizedBox(width: 8),
              const Text(
                "LIVE",
                style: TextStyle(
                  fontSize: 10,
                  fontWeight: FontWeight.w900,
                  color: Colors.grey,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildTabs() {
    final tabs = ['dashboard', 'routines', 'share'];
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: tabs.map((t) {
          final isActive = activeTab == t;
          return Padding(
            padding: const EdgeInsets.only(right: 8),
            child: InkWell(
              onTap: () => setState(() => activeTab = t),
              borderRadius: BorderRadius.circular(12),
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 20,
                  vertical: 10,
                ),
                decoration: BoxDecoration(
                  color: isActive ? Colors.white : const Color(0xFFF1F5F9),
                  borderRadius: BorderRadius.circular(12),
                  boxShadow: isActive
                      ? [
                          BoxShadow(
                            color: Colors.black.withOpacity(0.05),
                            blurRadius: 4,
                          ),
                        ]
                      : [],
                ),
                child: Text(
                  t.toUpperCase(),
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w900,
                    color: isActive ? const Color(0xFF4F46E5) : Colors.grey,
                  ),
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildAgendaItem(Appointment apt) {
    bool isGoogle = apt.status == 'GOOGLE';
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFF1F5F9)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.02),
            blurRadius: 5,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: const Color(0xFFEEF2FF), // Indigo-50
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(
                  "${DateFormat('HH:mm').format(apt.startTime)} - ${DateFormat('HH:mm').format(apt.endTime)}",
                  style: const TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.w900,
                    color: Color(0xFF4F46E5), // Indigo-600
                  ),
                ),
              ),
              if (isGoogle)
                const Icon(Icons.g_mobiledata, color: Colors.grey, size: 20)
              else if (apt.status == 'ACCEPTED')
                const Text(
                  "● Synced",
                  style: TextStyle(
                    fontSize: 10,
                    color: Colors.green,
                    fontWeight: FontWeight.bold,
                  ),
                )
              else if (apt.status == 'PENDING')
                const Text(
                  "● Pending",
                  style: TextStyle(
                    fontSize: 10,
                    color: Colors.amber,
                    fontWeight: FontWeight.bold,
                  ),
                ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            apt.title,
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
          ),
          if (!isGoogle)
            Padding(
              padding: const EdgeInsets.only(top: 4),
              child: Text(
                "Oleh: ${apt.guestName}",
                style: const TextStyle(fontSize: 10, color: Colors.grey),
              ),
            ),
        ],
      ),
    );
  }
}

// ==========================================
// WIDGET MODAL TERPISAH (Agar bisa update State jam)
// ==========================================
class QuickTaskModal extends StatefulWidget {
  final Function(String title, DateTime start, DateTime end) onSave;

  const QuickTaskModal({super.key, required this.onSave});

  @override
  State<QuickTaskModal> createState() => _QuickTaskModalState();
}

class _QuickTaskModalState extends State<QuickTaskModal> {
  final TextEditingController _titleController = TextEditingController();

  late DateTime _startDate;
  late DateTime _endDate;
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    // Default: Mulai jam berikutnya, durasi 1 jam
    final now = DateTime.now();
    _startDate = now.add(Duration(minutes: 60 - now.minute));
    _endDate = _startDate.add(const Duration(hours: 1));
  }

  // Fungsi Pilih Jam
  Future<void> _pickTime(bool isStart) async {
    final initial = isStart ? _startDate : _endDate;

    final TimeOfDay? picked = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(initial),
      builder: (context, child) {
        return Theme(
          data: ThemeData.light().copyWith(
            colorScheme: const ColorScheme.light(primary: Color(0xFF4F46E5)),
          ),
          child: child!,
        );
      },
    );

    if (picked != null) {
      setState(() {
        // Gabungkan Tanggal Hari Ini + Jam yang dipilih
        final now = DateTime.now();
        final newDateTime = DateTime(
          now.year,
          now.month,
          now.day,
          picked.hour,
          picked.minute,
        );

        if (isStart) {
          _startDate = newDateTime;
          // Validasi: Kalau start melebihi end, end dimajukan 1 jam
          if (_startDate.isAfter(_endDate)) {
            _endDate = _startDate.add(const Duration(hours: 1));
          }
        } else {
          // Validasi: End tidak boleh kurang dari start
          if (newDateTime.isBefore(_startDate)) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text("Jam selesai tidak boleh mundur!")),
            );
            return;
          }
          _endDate = newDateTime;
        }
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.only(
        top: 32,
        left: 32,
        right: 32,
        bottom: MediaQuery.of(context).viewInsets.bottom + 32,
      ),
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Center(
            child: Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: Colors.grey[200],
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          const SizedBox(height: 24),

          const Text(
            "⚡ Quick Task",
            style: TextStyle(fontSize: 24, fontWeight: FontWeight.w900),
          ),
          const SizedBox(height: 24),

          // Input Judul
          TextField(
            controller: _titleController,
            autofocus: true,
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
            decoration: InputDecoration(
              hintText: "Nama Agenda...",
              filled: true,
              fillColor: const Color(0xFFF8FAFC),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: BorderSide.none,
              ),
              contentPadding: const EdgeInsets.all(20),
            ),
          ),
          const SizedBox(height: 16),

          // Input Waktu (Sekarang Bisa Diklik!)
          Row(
            children: [
              Expanded(
                child: InkWell(
                  onTap: () => _pickTime(true),
                  borderRadius: BorderRadius.circular(16),
                  child: Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF8FAFC),
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: Colors.grey.shade200),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          "MULAI",
                          style: TextStyle(
                            fontSize: 10,
                            fontWeight: FontWeight.w900,
                            color: Colors.grey,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          DateFormat('HH:mm').format(_startDate),
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 16,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              const Icon(Icons.arrow_forward, color: Colors.grey, size: 16),
              const SizedBox(width: 12),
              Expanded(
                child: InkWell(
                  onTap: () => _pickTime(false),
                  borderRadius: BorderRadius.circular(16),
                  child: Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF8FAFC),
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: Colors.grey.shade200),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          "SELESAI",
                          style: TextStyle(
                            fontSize: 10,
                            fontWeight: FontWeight.w900,
                            color: Colors.grey,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          DateFormat('HH:mm').format(_endDate),
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 16,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),

          const SizedBox(height: 32),

          // Tombol Simpan
          SizedBox(
            width: double.infinity,
            height: 56,
            child: ElevatedButton(
              onPressed: _isSubmitting
                  ? null
                  : () async {
                      if (_titleController.text.isEmpty) return;

                      setState(() => _isSubmitting = true);

                      // Panggil callback parent untuk simpan ke API
                      await widget.onSave(
                        _titleController.text,
                        _startDate,
                        _endDate,
                      );

                      // Tutup modal
                      if (mounted) Navigator.pop(context);
                    },
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF4F46E5),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
                elevation: 0,
              ),
              child: _isSubmitting
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(
                        color: Colors.white,
                        strokeWidth: 2,
                      ),
                    )
                  : const Text(
                      "Simpan Agenda",
                      style: TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.w900,
                        fontSize: 16,
                      ),
                    ),
            ),
          ),
        ],
      ),
    );
  }
}
