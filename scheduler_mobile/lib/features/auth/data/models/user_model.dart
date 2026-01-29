// lib/features/auth/data/models/user_model.dart
import '../../domain/entities/user_entity.dart';

class UserModel extends UserEntity {
  UserModel({required super.email, super.name, super.token});

  // Factory untuk mengubah JSON dari Backend menjadi Object
  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      email: json['email'] ?? '',
      name: json['name'],
      // Kadang token ada di body, kadang di header (cookie).
      // Kita ambil dari body jika backend mengirimnya juga.
      token: json['session'],
    );
  }
}
