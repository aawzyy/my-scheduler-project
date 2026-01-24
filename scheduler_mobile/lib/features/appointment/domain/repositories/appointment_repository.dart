import 'package:fpdart/fpdart.dart';
import '../../../../core/errors/failures.dart';
import '../entities/appointment.dart';

abstract class AppointmentRepository {
  // Ambil semua jadwal
  Future<Either<Failure, List<Appointment>>> getAppointments();

  // Aksi Bos: Terima & Tolak
  Future<Either<Failure, void>> approveAppointment(String id);
  Future<Either<Failure, void>> rejectAppointment(String id);
}
