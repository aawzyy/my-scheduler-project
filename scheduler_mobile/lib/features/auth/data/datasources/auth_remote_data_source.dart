import 'dart:io';
import 'package:dio/dio.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:cookie_jar/cookie_jar.dart';
import '../models/user_model.dart';

abstract class AuthRemoteDataSource {
  Future<UserModel> loginWithGoogle();
}

class AuthRemoteDataSourceImpl implements AuthRemoteDataSource {
  final Dio dio;
  final PersistCookieJar cookieJar;
  final GoogleSignIn googleSignIn;

  AuthRemoteDataSourceImpl({
    required this.dio,
    required this.cookieJar,
    required this.googleSignIn,
  });

  @override
  Future<UserModel> loginWithGoogle() async {
    // 1. Proses Google Sign-In Native
    // Logout dulu untuk memastikan sesi bersih
    await googleSignIn.signOut();
    final GoogleSignInAccount? googleUser = await googleSignIn.signIn();

    if (googleUser == null) {
      throw Exception("Login dibatalkan oleh user");
    }

    final GoogleSignInAuthentication googleAuth =
        await googleUser.authentication;
    final String? idToken = googleAuth.idToken;
    final String? accessToken = googleAuth.accessToken;

    if (idToken == null) {
      throw Exception("Gagal mendapatkan ID Token dari Google");
    }

    // 2. Kirim ke Backend
    try {
      final response = await dio.post(
        '/api/mobile/auth/google',
        data: {'idToken': idToken, 'accessToken': accessToken},
      );

      if (response.statusCode == 200) {
        // 3. Simpan Cookie Manual (Logika Fix 401)
        List<String>? rawCookies = response.headers['set-cookie'];
        if (rawCookies != null && rawCookies.isNotEmpty) {
          List<Cookie> cookies = rawCookies
              .map((str) => Cookie.fromSetCookieValue(str))
              .toList();

          // Simpan ke topeles cookie global
          await cookieJar.saveFromResponse(
            Uri.parse('https://aawzyy.my.id'), // Sesuaikan base URL Anda
            cookies,
          );
        }
        return UserModel.fromJson(response.data);
      } else {
        throw Exception("Server menolak login: ${response.statusCode}");
      }
    } on DioException catch (e) {
      throw Exception("Gagal terhubung ke server: ${e.message}");
    }
  }
}
