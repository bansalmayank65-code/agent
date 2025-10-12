import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';

class StepCard extends StatelessWidget {
  final int stepNumber;
  final String title;
  final String description;
  final IconData icon;
  final bool isExpanded;
  final VoidCallback onToggle;
  final Widget child;

  const StepCard({
    Key? key,
    required this.stepNumber,
    required this.title,
    required this.description,
    required this.icon,
    required this.isExpanded,
    required this.onToggle,
    required this.child,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Column(
        children: [
          ListTile(
            leading: CircleAvatar(
              backgroundColor: Theme.of(context).primaryColor,
              child: Text(
                stepNumber.toString(),
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
            title: Row(
              children: [
                Icon(icon, size: 20),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    title,
                    style: const TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 16,
                    ),
                  ),
                ),
              ],
            ),
            subtitle: Padding(
              padding: const EdgeInsets.only(top: 4),
              child: Text(
                description,
                style: TextStyle(
                  color: Colors.grey.shade600,
                  fontSize: 14,
                ),
              ),
            ),
            trailing: Icon(
              isExpanded ? Icons.expand_less : Icons.expand_more,
            ),
            onTap: onToggle,
          ),
          if (isExpanded)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.grey.shade50,
                border: Border(
                  top: BorderSide(color: Colors.grey.shade200),
                ),
              ),
              child: child,
            )
                .animate()
                .fadeIn(duration: 300.ms)
                .slideY(begin: -0.2, end: 0, duration: 300.ms),
        ],
      ),
    );
  }
}