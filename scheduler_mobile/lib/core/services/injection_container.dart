import 'dart:io';

// Library Pihak Ketiga
import 'package:cookie_jar/cookie_jar.dart';
import 'package:dio/dio.dart';
import 'package:dio/io.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart';
import 'package:get_it/get_it.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:path_provider/path_provider.dart';

// ==========================================
// IMPORT FEATURE: AUTH (LOGIN)
// ==========================================
import '../../features/auth/data/datasources/auth_remote_data_source.dart';
import '../../features/auth/data/repositories/auth_repository_impl.dart';
import '../../features/auth/domain/repositories/auth_repository.dart';
import '../../features/auth/domain/usecases/login_google_usecase.dart';
import '../../features/auth/presentation/stores/login_store.dart';

// ==========================================
// IMPORT FEATURE: APPOINTMENT (JADWAL)
// ==========================================
import '../../features/appointment/data/datasources/appointment_remote_datasource.dart';
import '../../features/appointment/data/repositories/appointment_repository_impl.dart';
import '../../features/appointment/domain/repositories/appointment_repository.dart';
import '../../features/appointment/domain/usecases/approve_appointment.dart';
import '../../features/appointment/domain/usecases/get_appointments.dart';
import '../../features/appointment/domain/usecases/reject_appointment.dart';
import '../../features/appointment/domain/usecases/create_quick_task.dart'; // <--- IMPORT BARU
import '../../features/appointment/presentation/stores/appointment_store.dart';

// Variabel global untuk akses Injection di mana saja (misal: sl<Dio>())
final sl = GetIt.instance;

// URL Backend Production Anda
const String BASE_URL = "https://aawzyy.my.id";

// Fungsi init() ini WAJIB dipanggil di main.dart sebelum runApp()
Future<void> init() async {
  // ##################################################################
  // 1. EXTERNAL / CORE (Pondasi Aplikasi)
  // ##################################################################

  // --- A. Cookie Jar (Toples Cookie) ---
  final appDocDir = await getApplicationDocumentsDirectory();
  final String appDocPath = appDocDir.path;

  // Disimpan di folder hidden .cookies di dalam HP
  final jar = PersistCookieJar(storage: FileStorage("$appDocPath/.cookies/"));
  sl.registerLazySingleton<PersistCookieJar>(() => jar);

  // --- B. Google Sign In ---
  sl.registerLazySingleton(
    () => GoogleSignIn(
      // Client ID Web dari Backend
      serverClientId:
          "584227313143-usa6aj75fld5bbcc7flm63au3h5so0mt.apps.googleusercontent.com",
      scopes: ['email', 'https://www.googleapis.com/auth/calendar'],
    ),
  );

  // --- C. DIO (HTTP Client) ---
  sl.registerLazySingleton(() {
    final dio = Dio(
      BaseOptions(
        baseUrl: BASE_URL,
        connectTimeout: const Duration(seconds: 20),
        receiveTimeout: const Duration(seconds: 20),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      ),
    );

    // [PENTING] Pasang Cookie Manager (Otomatis simpan/baca session)
    dio.interceptors.add(CookieManager(sl<PersistCookieJar>()));

    // [DEBUGGING] Logger
    dio.interceptors.add(LogInterceptor(requestBody: true, responseBody: true));

    // [SSL HANDSHAKE FIX]
    dio.httpClientAdapter = IOHttpClientAdapter(
      createHttpClient: () {
        final client = HttpClient();
        client.badCertificateCallback =
            (X509Certificate cert, String host, int port) => true;
        return client;
      },
    );

    return dio;
  });

  // ##################################################################
  // 2. FEATURE: AUTHENTICATION (Login)
  // ##################################################################

  sl.registerLazySingleton<AuthRemoteDataSource>(
    () => AuthRemoteDataSourceImpl(
      dio: sl(),
      cookieJar: sl(),
      googleSignIn: sl(),
    ),
  );

  sl.registerLazySingleton<AuthRepository>(
    () => AuthRepositoryImpl(remoteDataSource: sl()),
  );

  sl.registerLazySingleton(() => LoginGoogleUseCase(sl()));

  sl.registerFactory(() => LoginStore(sl()));

  // ##################################################################
  // 3. FEATURE: APPOINTMENT (Jadwal)
  // ##################################################################

  // --- Data Source ---
  sl.registerLazySingleton<AppointmentRemoteDataSource>(
    () => AppointmentRemoteDataSourceImpl(dio: sl()),
  );

  // --- Repository ---
  sl.registerLazySingleton<AppointmentRepository>(
    () => AppointmentRepositoryImpl(remoteDataSource: sl()),
  );

  // --- Use Cases ---
  sl.registerLazySingleton(() => GetAppointments(sl()));
  sl.registerLazySingleton(() => ApproveAppointment(sl()));
  sl.registerLazySingleton(() => RejectAppointment(sl()));
  sl.registerLazySingleton(
    () => CreateQuickTask(sl()),
  ); // <--- REGISTRASI USECASE BARU

  // --- Store (MobX) ---
  sl.registerFactory(
    () => AppointmentStore(
      getAppointmentsUseCase: sl(),
      approveAppointmentUseCase: sl(),
      rejectAppointmentUseCase: sl(),
      createQuickTaskUseCase: sl(), // <--- MASUKKAN KE STORE
    ),
  );
}
