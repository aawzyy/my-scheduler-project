import 'package:flutter/material.dart';
import 'core/services/injection_container.dart' as di;
import 'features/appointment/presentation/pages/dashboard_page.dart';
import 'features/auth/presentation/pages/login_page.dart';
import 'package:cookie_jar/cookie_jar.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await di.init();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Scheduler App',
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.amber),
      ),
      home: const AuthCheck(),
    );
  }
}

// Widget sederhana buat cek login status
class AuthCheck extends StatefulWidget {
  const AuthCheck({super.key});

  @override
  State<AuthCheck> createState() => _AuthCheckState();
}

class _AuthCheckState extends State<AuthCheck> {
  bool? isLogged;

  @override
  void initState() {
    super.initState();
    checkSession();
  }

  void checkSession() async {
    final jar = di.sl<PersistCookieJar>();
    final cookies = await jar.loadForRequest(Uri.parse("https://aawzyy.my.id"));
    // Cek apakah ada JSESSIONID
    setState(() {
      isLogged = cookies.any((c) => c.name == 'JSESSIONID');
    });
  }

  @override
  Widget build(BuildContext context) {
    if (isLogged == null)
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    return isLogged! ? const DashboardPage() : const LoginPage();
  }
}
