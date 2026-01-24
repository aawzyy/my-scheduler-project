import 'package:fpdart/fpdart.dart';
import '../errors/failures.dart';

// Type: Tipe data yang dikembalikan jika SUKSES (misal: List<Appointment>)
// Params: Parameter yang dibutuhkan (misal: String id, atau NoParams)
abstract class UseCase<Type, Params> {
  Future<Either<Failure, Type>> call(Params params);
}

// Kelas dummy jika UseCase tidak butuh parameter
class NoParams {
  const NoParams();
}
