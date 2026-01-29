import 'package:equatable/equatable.dart';

class Appointment extends Equatable {
  final String id;
  final String guestName; // <--- INI YANG HILANG TADI
  final String title;
  final DateTime startTime;
  final DateTime endTime;
  final String status; // 'pending', 'approved', 'rejected'

  const Appointment({
    required this.id,
    required this.guestName, // Tambahkan ini
    required this.title,
    required this.startTime,
    required this.endTime,
    required this.status,
  });

  @override
  List<Object?> get props => [id, guestName, title, startTime, endTime, status];
}
