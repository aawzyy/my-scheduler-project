import 'package:fpdart/fpdart.dart';
import '../../../../core/usecases/usecase.dart';
import '../../../../core/errors/failures.dart';
import '../repositories/appointment_repository.dart';

class RejectAppointment implements UseCase<void, String> {
  final AppointmentRepository repository;

  RejectAppointment(this.repository);

  @override
  Future<Either<Failure, void>> call(String id) async {
    return await repository.rejectAppointment(id);
  }
}
