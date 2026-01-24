import 'package:flutter/material.dart';
import 'package:intl/intl.dart'; // Pastikan sudah install intl
import '../../domain/entities/appointment.dart';
import '../stores/appointment_store.dart';

class InboxWidget extends StatelessWidget {
  final List<Appointment> appointments;
  final AppointmentStore store;

  const InboxWidget({
    super.key,
    required this.appointments,
    required this.store,
  });

  @override
  Widget build(BuildContext context) {
    // Filter hanya PENDING
    final pendingList = appointments
        .where((element) => element.status == 'PENDING')
        .toList();

    if (pendingList.isEmpty) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Header
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
          child: Row(
            children: [
              const Text("🔔", style: TextStyle(fontSize: 20)),
              const SizedBox(width: 8),
              Text(
                "Inbox (${pendingList.length})",
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w900,
                  color: Colors.amber, // Sesuaikan warna dashboard web
                ),
              ),
            ],
          ),
        ),

        // Horizontal List (Swipe)
        SizedBox(
          height: 180, // Tinggi kartu
          child: ListView.separated(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            scrollDirection: Axis.horizontal,
            itemCount: pendingList.length,
            separatorBuilder: (_, __) => const SizedBox(width: 12),
            itemBuilder: (context, index) {
              final item = pendingList[index];
              return _buildCard(item, context);
            },
          ),
        ),
        const SizedBox(height: 20),
      ],
    );
  }

  Widget _buildCard(Appointment item, BuildContext context) {
    return Container(
      width: 280,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: Colors.amber.shade100),
        boxShadow: [
          BoxShadow(
            color: Colors.amber.withOpacity(0.1),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          // Info Atas
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  _badge(DateFormat('dd MMM').format(item.startTime)),
                  const SizedBox(width: 8),
                  _badge(DateFormat('HH:mm').format(item.startTime)),
                ],
              ),
              const SizedBox(height: 10),
              Text(
                item.title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 16,
                ),
              ),
              Text(
                "Oleh: ${item.requesterName}",
                style: TextStyle(color: Colors.grey.shade500, fontSize: 12),
              ),
            ],
          ),

          // Tombol Aksi
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: () => store.reject(item.id),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: Colors.red,
                    side: const BorderSide(color: Colors.red),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  child: const Text("Tolak", style: TextStyle(fontSize: 12)),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: ElevatedButton(
                  onPressed: () => store.approve(item.id),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.indigo,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  child: const Text("Terima", style: TextStyle(fontSize: 12)),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _badge(String text) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.grey.shade100,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        text,
        style: TextStyle(
          fontSize: 10,
          fontWeight: FontWeight.bold,
          color: Colors.grey.shade700,
        ),
      ),
    );
  }
}
