import '../../domain/entities/appointment.dart';

class AppointmentModel extends Appointment {
  const AppointmentModel({
    required super.id,
    required super.guestName,
    required super.title,
    required super.startTime,
    required super.endTime,
    required super.status,
  });

  factory AppointmentModel.fromJson(Map<String, dynamic> json) {
    return AppointmentModel(
      // 1. ID: Pastikan jadi String, kalau null kasih "0"
      id: (json['id'] ?? 0).toString(),

      // 2. GUEST NAME: Cek berbagai kemungkinan key, default "Tanpa Nama"
      guestName: json['guest_name'] ?? json['name'] ?? 'Tamu Tanpa Nama',

      // 3. TITLE: Kalau null, kasih "Tanpa Judul"
      title: json['title'] ?? 'Tanpa Judul',

      // 4. START TIME (CRITICAL FIX):
      // Cek dulu null nggak? Kalau null atau error parse, pakai Waktu Sekarang
      startTime: json['start_time'] != null
          ? DateTime.tryParse(json['start_time'].toString()) ?? DateTime.now()
          : DateTime.now(),

      // 5. END TIME (CRITICAL FIX):
      endTime: json['end_time'] != null
          ? DateTime.tryParse(json['end_time'].toString()) ??
                DateTime.now().add(const Duration(hours: 1))
          : DateTime.now().add(const Duration(hours: 1)),

      // 6. STATUS: Default "pending"
      status: json['status'] ?? 'pending',
    );
  }
}
