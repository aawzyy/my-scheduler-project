// lib/features/auth/presentation/stores/login_store.dart
import 'package:mobx/mobx.dart';
import '../../domain/usecases/login_google_usecase.dart'; // Import UseCase
import '../../domain/entities/user_entity.dart';

part 'login_store.g.dart';

class LoginStore = _LoginStore with _$LoginStore;

abstract class _LoginStore with Store {
  // Dependency diganti jadi UseCase
  final LoginGoogleUseCase _loginGoogleUseCase;

  _LoginStore(this._loginGoogleUseCase);

  @observable
  bool isLoading = false;

  @observable
  String? errorMessage;

  @observable
  UserEntity? user; // Kita bisa simpan data user yang login

  @action
  Future<void> login() async {
    isLoading = true;
    errorMessage = null;

    try {
      // Panggil UseCase
      user = await _loginGoogleUseCase.call();
    } catch (e) {
      errorMessage = e.toString();
    } finally {
      isLoading = false;
    }
  }
}
