import 'package:fpdart/fpdart.dart';
import '../../../../core/errors/failures.dart';
import '../repositories/appointment_repository.dart';

class ApproveAppointment {
  final AppointmentRepository repository;

  ApproveAppointment(this.repository);

  Future<Either<Failure, void>> call(String id) async {
    return await repository.approveAppointment(id);
  }
}
