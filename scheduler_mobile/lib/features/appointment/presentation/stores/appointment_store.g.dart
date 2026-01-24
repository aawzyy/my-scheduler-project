// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'appointment_store.dart';

// **************************************************************************
// StoreGenerator
// **************************************************************************

// ignore_for_file: non_constant_identifier_names, unnecessary_brace_in_string_interps, unnecessary_lambdas, prefer_expression_function_bodies, lines_longer_than_80_chars, avoid_as, avoid_annotating_with_dynamic, no_leading_underscores_for_local_identifiers

mixin _$AppointmentStore on _AppointmentStore, Store {
  late final _$appointmentsAtom = Atom(
    name: '_AppointmentStore.appointments',
    context: context,
  );

  @override
  ObservableList<Appointment> get appointments {
    _$appointmentsAtom.reportRead();
    return super.appointments;
  }

  @override
  set appointments(ObservableList<Appointment> value) {
    _$appointmentsAtom.reportWrite(value, super.appointments, () {
      super.appointments = value;
    });
  }

  late final _$isLoadingAtom = Atom(
    name: '_AppointmentStore.isLoading',
    context: context,
  );

  @override
  bool get isLoading {
    _$isLoadingAtom.reportRead();
    return super.isLoading;
  }

  @override
  set isLoading(bool value) {
    _$isLoadingAtom.reportWrite(value, super.isLoading, () {
      super.isLoading = value;
    });
  }

  late final _$errorMessageAtom = Atom(
    name: '_AppointmentStore.errorMessage',
    context: context,
  );

  @override
  String? get errorMessage {
    _$errorMessageAtom.reportRead();
    return super.errorMessage;
  }

  @override
  set errorMessage(String? value) {
    _$errorMessageAtom.reportWrite(value, super.errorMessage, () {
      super.errorMessage = value;
    });
  }

  late final _$fetchAppointmentsAsyncAction = AsyncAction(
    '_AppointmentStore.fetchAppointments',
    context: context,
  );

  @override
  Future<void> fetchAppointments() {
    return _$fetchAppointmentsAsyncAction.run(() => super.fetchAppointments());
  }

  late final _$approveAsyncAction = AsyncAction(
    '_AppointmentStore.approve',
    context: context,
  );

  @override
  Future<void> approve(String id) {
    return _$approveAsyncAction.run(() => super.approve(id));
  }

  late final _$rejectAsyncAction = AsyncAction(
    '_AppointmentStore.reject',
    context: context,
  );

  @override
  Future<void> reject(String id) {
    return _$rejectAsyncAction.run(() => super.reject(id));
  }

  @override
  String toString() {
    return '''
appointments: ${appointments},
isLoading: ${isLoading},
errorMessage: ${errorMessage}
    ''';
  }
}
