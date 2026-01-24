import '../../domain/entities/appointment.dart';

class AppointmentModel extends Appointment {
  const AppointmentModel({
    required super.id,
    required super.title,
    required super.description,
    required super.requesterName,
    required super.requesterEmail,
    required super.startTime,
    required super.endTime,
    required super.status,
  });

  factory AppointmentModel.fromJson(Map<String, dynamic> json) {
    return AppointmentModel(
      id: json['id'],
      title: json['title'] ?? 'No Title',
      description: json['description'] ?? '',
      requesterName: json['requesterName'] ?? 'Unknown',
      requesterEmail: json['requesterEmail'] ?? '',
      // Backend kirim format ISO (2025-01-24T10:00:00)
      startTime: DateTime.parse(json['startTime']),
      endTime: DateTime.parse(json['endTime']),
      status: json['status'] ?? 'PENDING',
    );
  }
}
