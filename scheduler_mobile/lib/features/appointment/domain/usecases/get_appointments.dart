import 'package:fpdart/fpdart.dart';
import '../../../../core/usecases/usecase.dart';
import '../../../../core/errors/failures.dart';
import '../entities/appointment.dart';
import '../repositories/appointment_repository.dart';

class GetAppointments implements UseCase<List<Appointment>, NoParams> {
  final AppointmentRepository repository;

  GetAppointments(this.repository);

  @override
  Future<Either<Failure, List<Appointment>>> call(NoParams params) async {
    return await repository.getAppointments();
  }
}
