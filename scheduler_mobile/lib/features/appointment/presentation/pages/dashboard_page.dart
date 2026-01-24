import 'package:flutter/material.dart';
import 'package:flutter_mobx/flutter_mobx.dart';
import 'package:intl/intl.dart';
import '../../../../core/services/injection_container.dart';
import '../stores/appointment_store.dart';
import '../widgets/inbox_widget.dart'; // Import widget inbox tadi

class DashboardPage extends StatefulWidget {
  const DashboardPage({super.key});

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends State<DashboardPage> {
  final AppointmentStore _store = sl<AppointmentStore>();

  @override
  void initState() {
    super.initState();
    _store.fetchAppointments();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF8FAFC), // Warna background soft
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        title: const Text(
          "Dashboard",
          style: TextStyle(color: Colors.black, fontWeight: FontWeight.w900),
        ),
        actions: [
          IconButton(
            onPressed: () => _store.fetchAppointments(),
            icon: const Icon(Icons.refresh, color: Colors.black),
          ),
        ],
      ),
      body: Observer(
        builder: (_) {
          if (_store.isLoading) {
            return const Center(child: CircularProgressIndicator());
          }

          if (_store.errorMessage != null) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.error_outline, size: 48, color: Colors.red),
                  const SizedBox(height: 10),
                  Text(_store.errorMessage!),
                  TextButton(
                    onPressed: () => _store.fetchAppointments(),
                    child: const Text("Coba Lagi"),
                  ),
                ],
              ),
            );
          }

          // Filter Agenda Harian (Hanya yang ACCEPTED)
          final agendaList = _store.appointments
              .where((e) => e.status == 'ACCEPTED')
              .toList();

          // Sort by time
          agendaList.sort((a, b) => a.startTime.compareTo(b.startTime));

          return SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 20),

                // 1. WIDGET INBOX (Horizontal Swipe)
                // Kita pass semua data, logic filter PENDING ada di dalam widget
                InboxWidget(appointments: _store.appointments, store: _store),

                // 2. AGENDA HARIAN (Vertical List)
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  child: Text(
                    "Agenda Harian",
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: Colors.grey.shade800,
                    ),
                  ),
                ),
                const SizedBox(height: 10),

                if (agendaList.isEmpty)
                  Container(
                    height: 200,
                    alignment: Alignment.center,
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.coffee,
                          size: 40,
                          color: Colors.grey.shade300,
                        ),
                        const SizedBox(height: 10),
                        Text(
                          "Tidak ada agenda.",
                          style: TextStyle(color: Colors.grey.shade400),
                        ),
                      ],
                    ),
                  )
                else
                  ListView.separated(
                    shrinkWrap:
                        true, // Agar bisa scroll di dalam SingleChildScrollView
                    physics: const NeverScrollableScrollPhysics(),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 20,
                      vertical: 10,
                    ),
                    itemCount: agendaList.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 12),
                    itemBuilder: (context, index) {
                      final item = agendaList[index];
                      return Container(
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(color: Colors.grey.shade200),
                        ),
                        child: Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                color: Colors.indigo.shade50,
                                borderRadius: BorderRadius.circular(12),
                              ),
                              child: Text(
                                DateFormat('HH:mm').format(item.startTime),
                                style: const TextStyle(
                                  fontWeight: FontWeight.w900,
                                  color: Colors.indigo,
                                ),
                              ),
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    item.title,
                                    style: const TextStyle(
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                  Text(
                                    item.requesterName,
                                    style: TextStyle(
                                      color: Colors.grey.shade500,
                                      fontSize: 12,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            const Icon(
                              Icons.check_circle,
                              color: Colors.green,
                              size: 18,
                            ),
                          ],
                        ),
                      );
                    },
                  ),

                const SizedBox(height: 40), // Padding bawah
              ],
            ),
          );
        },
      ),
    );
  }
}
