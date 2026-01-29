import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
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
    // Filter PENDING
    final pendingList = appointments
        .where((element) => element.status == 'PENDING')
        .toList();

    // Jika kosong, sembunyikan section ini (Sesuai Vue: v-if="pendingRequests.length > 0")
    if (pendingList.isEmpty) return const SizedBox.shrink();

    return Container(
      margin: const EdgeInsets.only(bottom: 24),
      padding: const EdgeInsets.symmetric(vertical: 20),
      decoration: BoxDecoration(
        color: const Color(0xFFFFFBEB), // bg-amber-50
        borderRadius: BorderRadius.circular(32), // rounded-[2rem]
        border: Border.all(color: const Color(0xFFFEF3C7)), // border-amber-100
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header Inbox
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 24),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Text("🔔", style: TextStyle(fontSize: 18)),
                        const SizedBox(width: 8),
                        Text(
                          "Inbox (${pendingList.length})",
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w900, // font-black
                            color: Color(0xFF78350F), // text-amber-900
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    const Text(
                      "TINJAU PERMINTAAN MASUK",
                      style: TextStyle(
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFFD97706), // text-amber-600
                        letterSpacing: 1.2,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // Horizontal List
          SizedBox(
            height: 200,
            child: ListView.separated(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              scrollDirection: Axis.horizontal,
              itemCount: pendingList.length,
              separatorBuilder: (_, __) => const SizedBox(width: 16),
              itemBuilder: (context, index) {
                return _buildCard(pendingList[index]);
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCard(Appointment item) {
    return Container(
      width: 280,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFFFEF3C7)), // border-amber-100
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.03),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Badges Tanggal & Jam
          Row(
            children: [
              _badge("📅 ${DateFormat('dd MMM').format(item.startTime)}"),
              const SizedBox(width: 8),
              _badge("⏰ ${DateFormat('HH:mm').format(item.startTime)}"),
            ],
          ),
          const SizedBox(height: 12),

          // Judul & Nama
          Text(
            item.title,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              fontWeight: FontWeight.w900, // font-black
              fontSize: 16,
              color: Color(0xFF1E293B), // text-slate-800
            ),
          ),
          const SizedBox(height: 4),
          Text.rich(
            TextSpan(
              text: "Oleh: ",
              style: const TextStyle(color: Colors.grey, fontSize: 12),
              children: [
                TextSpan(
                  text: item.guestName,
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF475569), // text-slate-600
                  ),
                ),
              ],
            ),
          ),

          const Spacer(),

          // Buttons
          Row(
            children: [
              Expanded(
                child: InkWell(
                  onTap: () => store.reject(item.id),
                  child: Container(
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      border: Border.all(color: const Color(0xFFFEF2F2)),
                      borderRadius: BorderRadius.circular(12),
                      color: Colors.white,
                    ),
                    child: const Text(
                      "Tolak",
                      style: TextStyle(
                        color: Color(0xFFF87171), // text-red-400
                        fontWeight: FontWeight.w900,
                        fontSize: 12,
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: InkWell(
                  onTap: () => store.approve(item.id),
                  child: Container(
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: const Color(0xFF1E293B), // bg-slate-900
                      borderRadius: BorderRadius.circular(12),
                      boxShadow: [
                        BoxShadow(
                          color: const Color(0xFFE2E8F0),
                          blurRadius: 8,
                          offset: const Offset(0, 4),
                        ),
                      ],
                    ),
                    child: const Text(
                      "Terima",
                      style: TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.w900,
                        fontSize: 12,
                      ),
                    ),
                  ),
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
        color: const Color(0xFFF1F5F9), // bg-slate-100
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        text,
        style: const TextStyle(
          fontSize: 10,
          fontWeight: FontWeight.w900,
          color: Color(0xFF475569), // text-slate-600
        ),
      ),
    );
  }
}
