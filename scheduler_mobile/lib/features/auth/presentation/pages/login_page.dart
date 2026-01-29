import 'package:flutter/material.dart';
import 'package:flutter_mobx/flutter_mobx.dart'; // Wajib untuk Observer
import 'package:mobx/mobx.dart';
import '../../../../core/services/injection_container.dart'; // Akses sl<>
import '../stores/login_store.dart';
import '../../../appointment/presentation/pages/dashboard_page.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  // 1. Ambil Store dari Injection Container
  final LoginStore _loginStore = sl<LoginStore>();

  // Disposer untuk mematikan reaksi saat halaman ditutup
  late ReactionDisposer _disposer;

  @override
  void initState() {
    super.initState();

    // 2. Pasang "Mata-mata" (Reaction)
    // Jika variable user di Store terisi (artinya sukses login), pindah halaman.
    _disposer = reaction((_) => _loginStore.user, (user) {
      if (user != null) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Selamat Datang, ${user.name ?? "User"}!')),
          );
          // Pindah ke Dashboard
          Navigator.of(context).pushReplacement(
            MaterialPageRoute(builder: (_) => const DashboardPage()),
          );
        }
      }
    });

    // Reaksi jika ada Error
    reaction((_) => _loginStore.errorMessage, (String? msg) {
      if (msg != null && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(msg), backgroundColor: Colors.red),
        );
      }
    });
  }

  @override
  void dispose() {
    _disposer(); // Matikan reaksi biar gak memori leak
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(
                Icons.calendar_month_outlined,
                size: 100,
                color: Colors.indigo,
              ),
              const SizedBox(height: 30),
              const Text(
                "Scheduler Owner",
                style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 50),

              // 3. UI yang bereaksi (Observer)
              // Bagian ini akan otomatis rebuild kalau isLoading berubah
              Observer(
                builder: (_) {
                  if (_loginStore.isLoading) {
                    return const Column(
                      children: [
                        CircularProgressIndicator(),
                        SizedBox(height: 20),
                        Text(
                          "Sedang Masuk...",
                          style: TextStyle(color: Colors.grey),
                        ),
                      ],
                    );
                  }

                  return SizedBox(
                    width: double.infinity,
                    height: 50,
                    child: ElevatedButton.icon(
                      onPressed: () {
                        // Panggil fungsi di Store (Clean!)
                        // UI tidak perlu tahu soal Dio/Token/Cookie
                        _loginStore.login();
                      },
                      icon: const Icon(Icons.login),
                      label: const Text("Masuk dengan Google"),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.white,
                        foregroundColor: Colors.black87,
                        elevation: 2,
                      ),
                    ),
                  );
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
