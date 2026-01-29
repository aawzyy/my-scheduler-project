import 'package:mobx/mobx.dart';
import '../../domain/entities/appointment.dart';
import '../../domain/usecases/get_appointments.dart';
import '../../domain/usecases/approve_appointment.dart';
import '../../domain/usecases/reject_appointment.dart';
import '../../domain/usecases/create_quick_task.dart'; // <--- IMPORT BARU
import '../../../../core/usecases/usecase.dart';

part 'appointment_store.g.dart';

class AppointmentStore = _AppointmentStore with _$AppointmentStore;

abstract class _AppointmentStore with Store {
  final GetAppointments _getAppointmentsUseCase;
  final ApproveAppointment _approveAppointmentUseCase;
  final RejectAppointment _rejectAppointmentUseCase;
  final CreateQuickTask _createQuickTaskUseCase; // <--- FIELD BARU

  _AppointmentStore({
    required GetAppointments getAppointmentsUseCase,
    required ApproveAppointment approveAppointmentUseCase,
    required RejectAppointment rejectAppointmentUseCase,
    required CreateQuickTask createQuickTaskUseCase, // <--- PARAMETER BARU
  }) : _getAppointmentsUseCase = getAppointmentsUseCase,
       _approveAppointmentUseCase = approveAppointmentUseCase,
       _rejectAppointmentUseCase = rejectAppointmentUseCase,
       _createQuickTaskUseCase = createQuickTaskUseCase;

  @observable
  bool isLoading = false;

  @observable
  String? errorMessage;

  @observable
  ObservableList<Appointment> appointments = ObservableList<Appointment>();

  // --- 1. Load Data ---
  @action
  Future<void> loadAppointments() async {
    isLoading = true;
    errorMessage = null;

    try {
      final result = await _getAppointmentsUseCase.call(NoParams());

      result.fold(
        (failure) {
          errorMessage = "Gagal memuat data: ${failure.message}";
        },
        (data) {
          appointments.clear();
          appointments.addAll(data);
        },
      );
    } catch (e) {
      errorMessage = e.toString();
    } finally {
      isLoading = false;
    }
  }

  // --- 2. Approve ---
  @action
  Future<void> approve(String id) async {
    isLoading = true;
    final result = await _approveAppointmentUseCase.call(id);
    result.fold((l) => errorMessage = l.message, (r) => loadAppointments());
    isLoading = false;
  }

  // --- 3. Reject ---
  @action
  Future<void> reject(String id) async {
    isLoading = true;
    final result = await _rejectAppointmentUseCase.call(id);
    result.fold((l) => errorMessage = l.message, (r) => loadAppointments());
    isLoading = false;
  }

  // --- 4. Create Quick Task (Fungsi Baru) ---
  @action
  Future<bool> createQuickTask(
    String title,
    DateTime start,
    DateTime end,
  ) async {
    isLoading = true;
    errorMessage = null;

    // Siapkan Payload sesuai API Vue
    final payload = {
      "title": title,
      "description": "Quick task from Mobile",
      "startTime": start.toIso8601String(),
      "endTime": end.toIso8601String(),
      "requesterEmail": "owner@scheduler.com", // Default Owner
      "requesterName": "Owner",
    };

    final result = await _createQuickTaskUseCase.call(payload);

    bool success = false;
    await result.fold(
      (l) async {
        errorMessage = l.message;
        success = false;
      },
      (r) async {
        // Jika sukses, reload data agar agenda baru muncul
        await loadAppointments();
        success = true;
      },
    );

    isLoading = false;
    return success;
  }
}
