import 'package:equatable/equatable.dart';

abstract class Failure extends Equatable {
  final String message;

  const Failure(this.message);

  @override
  List<Object> get props => [message];
}

// Gagal koneksi ke Server (500, 404, dll)
class ServerFailure extends Failure {
  const ServerFailure(super.message);
}

// Gagal koneksi Internet
class ConnectionFailure extends Failure {
  const ConnectionFailure() : super('Tidak ada koneksi internet');
}

// Gagal cache lokal (jika ada)
class CacheFailure extends Failure {
  const CacheFailure() : super('Gagal memuat data lokal');
}

// Gagal validasi input user
class ValidationFailure extends Failure {
  const ValidationFailure(super.message);
}
