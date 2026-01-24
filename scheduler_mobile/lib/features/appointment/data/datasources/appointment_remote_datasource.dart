import 'package:dio/dio.dart';
import '../models/appointment_model.dart';

abstract class AppointmentRemoteDataSource {
  Future<List<AppointmentModel>> getAppointments();
  Future<void> updateStatus(String id, String action); // approve / reject
}

class AppointmentRemoteDataSourceImpl implements AppointmentRemoteDataSource {
  final Dio dio;

  AppointmentRemoteDataSourceImpl({required this.dio});

  @override
  Future<List<AppointmentModel>> getAppointments() async {
    try {
      // Panggil endpoint yang sama dengan Frontend Vue
      final response = await dio.get('/api/appointments');

      if (response.statusCode == 200) {
        List data = response.data;
        return data.map((json) => AppointmentModel.fromJson(json)).toList();
      } else {
        throw Exception('Failed to load appointments');
      }
    } catch (e) {
      throw Exception(e.toString());
    }
  }

  @override
  Future<void> updateStatus(String id, String action) async {
    // action = "approve" atau "reject"
    // Sesuai controller backend: /api/appointments/{id}/approve
    try {
      await dio.post('/api/appointments/$id/$action');
    } catch (e) {
      throw Exception('Gagal update status: $e');
    }
  }
}
