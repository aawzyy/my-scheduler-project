import 'package:dio/dio.dart';
import '../models/appointment_model.dart';

abstract class AppointmentRemoteDataSource {
  Future<List<AppointmentModel>> getAppointments();
  Future<void> createQuickTask(Map<String, dynamic> payload);
  Future<void> approveAppointment(String id);
  Future<void> rejectAppointment(String id);
}

class AppointmentRemoteDataSourceImpl implements AppointmentRemoteDataSource {
  final Dio dio;

  AppointmentRemoteDataSourceImpl({required this.dio});

  @override
  Future<List<AppointmentModel>> getAppointments() async {
    try {
      final response = await dio.get('/api/dashboard/combined');
      List<dynamic> data = response.data;
      return data.map((json) => AppointmentModel.fromJson(json)).toList();
    } on DioException catch (e) {
      throw Exception("Gagal load data: ${e.message}");
    }
  }

  @override
  Future<void> createQuickTask(Map<String, dynamic> payload) async {
    try {
      await dio.post('/api/appointments/tasks/quick', data: payload);
    } on DioException catch (e) {
      throw Exception("Gagal buat task: ${e.message}");
    }
  }

  // --- PERBAIKAN APPROVE ---
  @override
  Future<void> approveAppointment(String id) async {
    try {
      // TRICK: Gunakan ResponseType.plain agar Dio tidak error saat terima body kosong
      final response = await dio.post(
        '/api/appointments/$id/approve',
        options: Options(responseType: ResponseType.plain),
      );

      // Cek manual: Jika status 200 atau 201, anggap sukses
      if (response.statusCode == 200 || response.statusCode == 201) {
        return; // Sukses!
      } else {
        throw Exception("Gagal: Status Code ${response.statusCode}");
      }
    } on DioException catch (e) {
      // Kadang Dio tetap anggap error meski sukses jika response kosong
      // Kita cek lagi di sini
      if (e.response?.statusCode == 200 || e.response?.statusCode == 201) {
        return; // Abaikan error, anggap sukses
      }
      throw Exception("Gagal approve: ${e.message}");
    }
  }

  // --- PERBAIKAN REJECT ---
  @override
  Future<void> rejectAppointment(String id) async {
    try {
      final response = await dio.post(
        '/api/appointments/$id/reject',
        options: Options(responseType: ResponseType.plain),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        return;
      } else {
        throw Exception("Gagal: Status Code ${response.statusCode}");
      }
    } on DioException catch (e) {
      if (e.response?.statusCode == 200 || e.response?.statusCode == 201) {
        return;
      }
      throw Exception("Gagal reject: ${e.message}");
    }
  }
}
