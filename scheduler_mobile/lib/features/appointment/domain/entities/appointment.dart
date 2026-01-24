import 'package:equatable/equatable.dart';

class Appointment extends Equatable {
  final String id;
  final String title;
  final String description;
  final String requesterName;
  final String requesterEmail;
  final DateTime startTime;
  final DateTime endTime;
  final String status; // PENDING, ACCEPTED, REJECTED

  const Appointment({
    required this.id,
    required this.title,
    required this.description,
    required this.requesterName,
    required this.requesterEmail,
    required this.startTime,
    required this.endTime,
    required this.status,
  });

  @override
  List<Object?> get props => [id, title, requesterName, startTime, status];
}
