import 'package:fpdart/fpdart.dart';
import '../../../../core/errors/exceptions.dart';
import '../../../../core/errors/failures.dart';
import '../../domain/entities/appointment.dart';
import '../../domain/repositories/appointment_repository.dart';
import '../datasources/appointment_remote_datasource.dart';

class AppointmentRepositoryImpl implements AppointmentRepository {
  final AppointmentRemoteDataSource remoteDataSource;

  AppointmentRepositoryImpl({required this.remoteDataSource});

  @override
  Future<Either<Failure, List<Appointment>>> getAppointments() async {
    try {
      final result = await remoteDataSource.getAppointments();
      return Right(result);
    } on ServerException catch (e) {
      return Left(ServerFailure(e.message ?? 'Terjadi kesalahan server'));
    } catch (e) {
      return Left(ServerFailure(e.toString()));
    }
  }

  @override
  Future<Either<Failure, void>> approveAppointment(String id) async {
    try {
      await remoteDataSource.updateStatus(id, 'ACCEPTED');
      return const Right(null);
    } on ServerException catch (e) {
      return Left(ServerFailure(e.message ?? 'Gagal menyetujui'));
    }
  }

  @override
  Future<Either<Failure, void>> rejectAppointment(String id) async {
    try {
      await remoteDataSource.updateStatus(id, 'REJECTED');
      return const Right(null);
    } on ServerException catch (e) {
      return Left(ServerFailure(e.message ?? 'Gagal menolak'));
    }
  }
}
