// lib/features/auth/domain/usecases/login_google_usecase.dart
import '../entities/user_entity.dart';
import '../repositories/auth_repository.dart';

class LoginGoogleUseCase {
  final AuthRepository repository;

  LoginGoogleUseCase(this.repository);

  // UseCase biasanya punya fungsi 'call' atau 'execute'
  Future<UserEntity> call() async {
    return await repository.loginWithGoogle();
  }
}
