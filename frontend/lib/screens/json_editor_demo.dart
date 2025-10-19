import 'package:flutter/material.dart';
import '../widgets/json_editor_viewer.dart';
import '../widgets/common/developer_footer.dart';

/// Demo page showcasing the JSON Editor + Viewer capabilities
class JsonEditorDemo extends StatelessWidget {
  const JsonEditorDemo({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('JSON Editor + Viewer Demo'),
        backgroundColor: const Color(0xFF2563EB),
        foregroundColor: Colors.white,
        elevation: 0,
      ),
      body: const SingleChildScrollView(
        padding: EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '🧩 JSON Editor + Viewer Features',
              style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
            ),
            SizedBox(height: 16),
            _FeatureCard(
              title: '✏️ Live JSON Editing',
              description: 'Edit JSON with syntax highlighting and real-time validation',
              icon: Icons.edit,
              color: Color(0xFF059669),
            ),
            SizedBox(height: 12),
            _FeatureCard(
              title: '🌳 Tree Viewer',
              description: 'Visualize JSON as an expandable/collapsible tree structure',
              icon: Icons.account_tree,
              color: Color(0xFF2563EB),
            ),
            SizedBox(height: 12),
            _FeatureCard(
              title: '🔧 Formatting Tools',
              description: 'Pretty print, minify, copy to clipboard, and validation',
              icon: Icons.build,
              color: Color(0xFF7C3AED),
            ),
            SizedBox(height: 12),
            _FeatureCard(
              title: '📱 Responsive Design',
              description: 'Split view on desktop, tabbed view on mobile',
              icon: Icons.devices,
              color: Color(0xFFDC2626),
            ),
            SizedBox(height: 24),
            Text(
              '🚀 Try it out:',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            SizedBox(height: 16),
            SizedBox(
              height: 600,
              child: JsonEditorViewer(
                showToolbar: true,
                splitView: true,
              ),
            ),
            SizedBox(height: 32),
            DeveloperFooter(),
          ],
        ),
      ),
    );
  }
}

class _FeatureCard extends StatelessWidget {
  final String title;
  final String description;
  final IconData icon;
  final Color color;

  const _FeatureCard({
    required this.title,
    required this.description,
    required this.icon,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: color.withOpacity(0.1),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Icon(icon, color: color, size: 24),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    description,
                    style: TextStyle(
                      fontSize: 14,
                      color: Colors.grey[600],
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}