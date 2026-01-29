// 1. PENTING: Gunakan fpdart (bukan dartz) agar cocok dengan Repository
import 'package:fpdart/fpdart.dart';

import '../../../../core/errors/failures.dart';
import '../repositories/appointment_repository.dart';

class CreateQuickTask {
  final AppointmentRepository repository;

  CreateQuickTask(this.repository);

  // 2. Pastikan return type sesuai dengan fpdart
  // 3. Wajib gunakan 'async' karena ada 'await' di dalamnya
  Future<Either<Failure, void>> call(Map<String, dynamic> payload) async {
    return await repository.createQuickTask(payload);
  }
}
