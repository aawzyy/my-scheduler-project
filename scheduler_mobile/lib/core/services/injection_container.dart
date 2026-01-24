import 'dart:io';
import 'package:cookie_jar/cookie_jar.dart';
import 'package:dio/dio.dart';
import 'package:dio/io.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart';
import 'package:get_it/get_it.dart';
import 'package:path_provider/path_provider.dart';

// Import features (sesuaikan path nanti)
import '../../features/appointment/data/datasources/appointment_remote_datasource.dart';
import '../../features/appointment/data/repositories/appointment_repository_impl.dart';
import '../../features/appointment/domain/repositories/appointment_repository.dart';
import '../../features/appointment/domain/usecases/approve_appointment.dart';
import '../../features/appointment/domain/usecases/get_appointments.dart';
import '../../features/appointment/domain/usecases/reject_appointment.dart';
import '../../features/appointment/presentation/stores/appointment_store.dart';

final sl = GetIt.instance;

// URL Backend Anda
const String BASE_URL = "https://aawzyy.my.id";

Future<void> init() async {
  // ======================
  // 1. FEATURE: APPOINTMENT
  // ======================

  // Store
  sl.registerFactory(
    () => AppointmentStore(
      getAppointmentsUseCase: sl(),
      approveAppointmentUseCase: sl(),
      rejectAppointmentUseCase: sl(),
    ),
  );

  // UseCases
  sl.registerLazySingleton(() => GetAppointments(sl()));
  sl.registerLazySingleton(() => ApproveAppointment(sl()));
  sl.registerLazySingleton(() => RejectAppointment(sl()));

  // Repository
  sl.registerLazySingleton<AppointmentRepository>(
    () => AppointmentRepositoryImpl(remoteDataSource: sl()),
  );

  // DataSource
  sl.registerLazySingleton<AppointmentRemoteDataSource>(
    () => AppointmentRemoteDataSourceImpl(dio: sl()),
  );

  // ======================
  // 2. CORE: NETWORK (DIO + COOKIE)
  // ======================

  // Siapkan tempat simpan Cookie di HP
  final appDocDir = await getApplicationDocumentsDirectory();
  final String appDocPath = appDocDir.path;
  final jar = PersistCookieJar(storage: FileStorage("$appDocPath/.cookies/"));
  sl.registerLazySingleton<PersistCookieJar>(() => jar);

  // Setup Dio
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

    // 1. Pasang Cookie Manager (Biar session Spring Boot tersimpan)
    dio.interceptors.add(CookieManager(sl<PersistCookieJar>()));

    // 2. Bypass SSL (Untuk handle sertifikat self-signed/error handshake)
    dio.httpClientAdapter = IOHttpClientAdapter(
      createHttpClient: () {
        final client = HttpClient();
        client.badCertificateCallback =
            (X509Certificate cert, String host, int port) => true;
        return client;
      },
    );

    // 3. Logger
    dio.interceptors.add(LogInterceptor(responseBody: true, requestBody: true));

    return dio;
  });
}
