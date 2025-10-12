import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/instruction_validation_provider.dart';

class InstructionValidationRedirectButton extends StatelessWidget {
  const InstructionValidationRedirectButton({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = context.read<InstructionValidationProvider>();
    return Column(
      children: [
        ElevatedButton.icon(
          onPressed: () => provider.redirect(),
          icon: const Icon(Icons.open_in_new),
            label: const Text('Open Instruction Validation Tool'),
          style: ElevatedButton.styleFrom(
            backgroundColor: const Color(0xFF667eea),
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 16),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
            elevation: 6,
          ),
        ),
        const SizedBox(height: 12),
        Wrap(
          alignment: WrapAlignment.center,
          spacing: 8,
          runSpacing: 8,
          children: [
            OutlinedButton.icon(
              onPressed: () async {
                await provider.redirectWithPolicy('mayank');
                // Best-effort feedback (will show briefly before navigation replaces page)
                try {
                  // ignore: use_build_context_synchronously
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Policy "mayank" copied – opening tool...')),
                  );
                } catch (_) {}
              },
              icon: const Icon(Icons.content_paste_go),
              label: const Text('Copy & Open with policy: mayank'),
              style: OutlinedButton.styleFrom(
                foregroundColor: const Color(0xFF667eea),
                side: const BorderSide(color: Color(0xFF667eea)),
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              ),
            ),
          ],
        ),
      ],
    );
  }
}
