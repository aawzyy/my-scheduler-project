// lib/features/auth/domain/entities/user_entity.dart
class UserEntity {
  final String email;
  final String? name;
  final String? token; // Session ID atau Token

  UserEntity({required this.email, this.name, this.token});
}
