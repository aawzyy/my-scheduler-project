import 'package:flutter/material.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:dio/dio.dart';
import 'package:cookie_jar/cookie_jar.dart';
import '../../../../core/services/injection_container.dart';
import '../../../appointment/presentation/pages/dashboard_page.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  // Inisialisasi Google Sign In
  final GoogleSignIn _googleSignIn = GoogleSignIn(
    // Scopes wajib agar backend bisa akses kalender
    scopes: ['email', 'https://www.googleapis.com/auth/calendar'],
  );

  bool isLoading = false;

  Future<void> _handleGoogleSignIn() async {
    setState(() => isLoading = true);
    try {
      // 1. Buka Popup Login Native Android (Anti-Blokir!)
      final GoogleSignInAccount? googleUser = await _googleSignIn.signIn();

      if (googleUser == null) {
        setState(() => isLoading = false);
        return; // User batal login
      }

      // 2. Ambil Token dari Google
      final GoogleSignInAuthentication googleAuth =
          await googleUser.authentication;
      final String? idToken = googleAuth.idToken;
      final String? accessToken = googleAuth.accessToken;

      if (idToken != null) {
        await _sendTokenToBackend(idToken, accessToken);
      }
    } catch (error) {
      print(error);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Login Gagal: $error')));
      setState(() => isLoading = false);
    }
  }

  Future<void> _sendTokenToBackend(String idToken, String? accessToken) async {
    final dio = sl<Dio>(); // Dio yang sudah di-inject CookieJar

    try {
      // 3. Kirim Token ke Backend Spring Boot
      final response = await dio.post(
        '/api/mobile/auth/google',
        data: {'idToken': idToken, 'accessToken': accessToken},
      );

      if (response.statusCode == 200) {
        print(">>> LOGIN SUKSES! Session tersimpan otomatis oleh Dio.");

        // 4. Pindah ke Dashboard
        if (mounted) {
          Navigator.of(context).pushReplacement(
            MaterialPageRoute(builder: (_) => const DashboardPage()),
          );
        }
      }
    } catch (e) {
      print(">>> API ERROR: $e");
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Gagal verifikasi di server')),
      );
    } finally {
      setState(() => isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Center(
        child: isLoading
            ? const CircularProgressIndicator()
            : Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(
                    Icons.calendar_today,
                    size: 80,
                    color: Colors.indigo,
                  ),
                  const SizedBox(height: 20),
                  const Text(
                    "Login Owner",
                    style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 40),
                  ElevatedButton.icon(
                    onPressed: _handleGoogleSignIn,
                    icon: const Icon(Icons.login),
                    label: const Text("Masuk dengan Google"),
                    style: ElevatedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 30,
                        vertical: 15,
                      ),
                    ),
                  ),
                ],
              ),
      ),
    );
  }
}
