import 'package:mobx/mobx.dart';
import '../../../../core/usecases/usecase.dart'; // Import NoParams dari sini
import '../../domain/entities/appointment.dart';
import '../../domain/usecases/approve_appointment.dart';
import '../../domain/usecases/get_appointments.dart';
import '../../domain/usecases/reject_appointment.dart';

part 'appointment_store.g.dart';

class AppointmentStore = _AppointmentStore with _$AppointmentStore;

abstract class _AppointmentStore with Store {
  final GetAppointments _getAppointmentsUseCase;
  final ApproveAppointment _approveAppointmentUseCase;
  final RejectAppointment _rejectAppointmentUseCase;

  // Constructor menerima 3 UseCase
  _AppointmentStore({
    required GetAppointments getAppointmentsUseCase,
    required ApproveAppointment approveAppointmentUseCase,
    required RejectAppointment rejectAppointmentUseCase,
  }) : _getAppointmentsUseCase = getAppointmentsUseCase,
       _approveAppointmentUseCase = approveAppointmentUseCase,
       _rejectAppointmentUseCase = rejectAppointmentUseCase;

  // --- OBSERVABLES ---

  @observable
  ObservableList<Appointment> appointments = ObservableList<Appointment>();

  @observable
  bool isLoading = false;

  @observable
  String? errorMessage;

  // --- ACTIONS ---

  @action
  Future<void> fetchAppointments() async {
    isLoading = true;
    errorMessage = null;

    // FIX: Tambahkan const NoParams()
    final result = await _getAppointmentsUseCase.call(const NoParams());

    result.fold(
      (failure) {
        errorMessage = failure.message;
        isLoading = false;
      },
      (data) {
        appointments.clear();
        appointments.addAll(data);
        isLoading = false;
      },
    );
  }

  @action
  Future<void> approve(String id) async {
    isLoading = true;
    final result = await _approveAppointmentUseCase.call(id);

    result.fold(
      (failure) {
        errorMessage = failure.message;
        isLoading = false;
      },
      (_) {
        // Jika sukses, reload data agar status terupdate
        fetchAppointments();
      },
    );
  }

  @action
  Future<void> reject(String id) async {
    isLoading = true;
    final result = await _rejectAppointmentUseCase.call(id);

    result.fold(
      (failure) {
        errorMessage = failure.message;
        isLoading = false;
      },
      (_) {
        // Jika sukses, reload data
        fetchAppointments();
      },
    );
  }
}
