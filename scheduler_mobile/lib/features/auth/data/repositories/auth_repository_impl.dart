import 'package:scheduler_mobile/features/auth/data/models/user_model.dart';

import '../../domain/repositories/auth_repository.dart';
import '../datasources/auth_remote_data_source.dart';

class AuthRepositoryImpl implements AuthRepository {
  final AuthRemoteDataSource remoteDataSource;

  AuthRepositoryImpl({required this.remoteDataSource});

  @override
  Future<UserModel> loginWithGoogle() async {
    return await remoteDataSource.loginWithGoogle();
  }
}
