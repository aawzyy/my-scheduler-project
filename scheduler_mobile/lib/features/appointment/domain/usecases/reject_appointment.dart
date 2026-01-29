import 'package:fpdart/fpdart.dart';
import '../../../../core/errors/failures.dart';
import '../repositories/appointment_repository.dart';

class RejectAppointment {
  final AppointmentRepository repository;

  RejectAppointment(this.repository);

  Future<Either<Failure, void>> call(String id) async {
    return await repository.rejectAppointment(id);
  }
}
