import 'package:fpdart/fpdart.dart';
import '../../../../core/usecases/usecase.dart';
import '../../../../core/errors/failures.dart';
import '../repositories/appointment_repository.dart';

class ApproveAppointment implements UseCase<void, String> {
  final AppointmentRepository repository;

  ApproveAppointment(this.repository);

  @override
  Future<Either<Failure, void>> call(String id) async {
    return await repository.approveAppointment(id);
  }
}
