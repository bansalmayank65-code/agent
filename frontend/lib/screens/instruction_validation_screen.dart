import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/instruction_validation_provider.dart';
import '../widgets/gradient_background.dart';
import '../widgets/custom_app_bar.dart';
import '../widgets/instruction_validation_redirect_button.dart';
import '../widgets/app_footer.dart';

/// Minimal redirect-only screen. Legacy validation form removed.
class InstructionValidationScreen extends StatelessWidget {
  const InstructionValidationScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => InstructionValidationProvider(),
      child: Scaffold(
        body: GradientBackground(
          child: Column(
            children: [
              CustomAppBar(
                title: 'Turing Tooling',
                onBackPressed: () => Navigator.of(context).pop(),
              ),
              Expanded(
                child: SingleChildScrollView(
                  child: Center(
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 600),
                      child: Column(
                        children: const [
                          _RedirectCard(),
                          SizedBox(height: 24),
                          AppFooter(),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _RedirectCard extends StatelessWidget {
  const _RedirectCard();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      elevation: 10,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 40),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Container(
              width: 90,
              height: 90,
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF667eea), Color(0xFF764ba2)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(45),
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFF667eea).withValues(alpha: 0.35),
                    blurRadius: 22,
                    offset: const Offset(0, 12),
                  ),
                ],
              ),
              child: const Icon(Icons.open_in_new, size: 46, color: Colors.white),
            ),
            const SizedBox(height: 32),
            Text(
              'Instruction Validation Moved',
              style: theme.textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.bold,
                color: const Color(0xFF4a5568),
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 16),
            Text(
              'This workflow now lives in the dedicated external tool with the latest features and updates.',
              style: theme.textTheme.bodyMedium?.copyWith(
                height: 1.4,
                color: Colors.grey[700],
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 32),
            const InstructionValidationRedirectButton(),
            const SizedBox(height: 16),
            TextButton.icon(
              onPressed: () => context.read<InstructionValidationProvider>().redirect(),
              icon: const Icon(Icons.arrow_forward),
              label: const Text('Automatically redirect now'),
            ),
            const SizedBox(height: 8),
            Text(
              'You will open the validation workspace in a new browser tab/window.',
              style: theme.textTheme.bodySmall?.copyWith(color: Colors.grey[600]),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}