import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/task_provider.dart';

/// Widget for Number of Edges section
class NumberOfEdgesSectionWidget extends StatelessWidget {
  const NumberOfEdgesSectionWidget({super.key});

  /// Build complexity distribution row
  Widget _buildComplexityRow(String label, String percentage, Color color) {
    return Row(
      children: [
        Container(
          width: 12,
          height: 12,
          decoration: BoxDecoration(
            color: color.withValues(alpha: 0.2),
            border: Border.all(color: color),
            borderRadius: BorderRadius.circular(2),
          ),
        ),
        const SizedBox(width: 8),
        Text(
          label,
          style: const TextStyle(fontSize: 12),
        ),
        const Spacer(),
        Text(
          percentage,
          style: TextStyle(
            fontSize: 12, 
            fontWeight: FontWeight.bold,
            color: color,
          ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<TaskProvider>(
      builder: (context, provider, child) {
        // Calculate edge count from the current edges
        final edgeCount = provider.task.edges.length;
        
        // Determine complexity based on the rules
        String complexityLevel;
        Color complexityColor;
        String complexityDescription;
        
        if (edgeCount < 7) {
          complexityLevel = 'INVALID';
          complexityColor = Colors.red;
          complexityDescription = 'Task is not valid - minimum 7 edges required';
        } else if (edgeCount >= 7 && edgeCount <= 12) {
          complexityLevel = 'MEDIUM';
          complexityColor = Colors.orange;
          complexityDescription = 'Medium complexity (7-12 edges): 20% of tasks';
        } else if (edgeCount >= 13 && edgeCount <= 15) {
          complexityLevel = 'HARD';
          complexityColor = Colors.blue;
          complexityDescription = 'Hard complexity (13-15 edges): 50% of tasks';
        } else {
          complexityLevel = 'EXPERT';
          complexityColor = Colors.purple;
          complexityDescription = 'Expert complexity (16+ edges): 30% of tasks';
        }

        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.analytics, color: Color(0xFF059669)),
                const SizedBox(width: 8),
                const Text(
                  'Number of Edges Analysis',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const SizedBox(height: 16),
            
            // Edge count display
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: const Color(0xFF059669).withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: const Color(0xFF059669).withValues(alpha: 0.3)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.account_tree, color: const Color(0xFF059669), size: 20),
                      const SizedBox(width: 8),
                      const Text(
                        'Current Edge Count',
                        style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '$edgeCount edges',
                    style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Calculated from current edges JSON',
                    style: TextStyle(fontSize: 12, color: Colors.grey[600]),
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: 16),
            
            // Complexity analysis
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: complexityColor.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: complexityColor.withValues(alpha: 0.3)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.assessment, color: complexityColor, size: 20),
                      const SizedBox(width: 8),
                      Text(
                        'Task Complexity: $complexityLevel',
                        style: TextStyle(
                          fontWeight: FontWeight.bold, 
                          fontSize: 14,
                          color: complexityColor,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    complexityDescription,
                    style: const TextStyle(fontSize: 12),
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: 16),
            
            // Complexity distribution reference
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.grey[50],
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.grey[300]!),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.info_outline, color: Colors.grey[600], size: 20),
                      const SizedBox(width: 8),
                      Text(
                        'Complexity Distribution Guidelines',
                        style: TextStyle(
                          fontWeight: FontWeight.bold, 
                          fontSize: 14,
                          color: Colors.grey[700],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  _buildComplexityRow('Medium (7-12 edges)', '20%', Colors.orange),
                  const SizedBox(height: 6),
                  _buildComplexityRow('Hard (13-15 edges)', '50%', Colors.blue),
                  const SizedBox(height: 6),
                  _buildComplexityRow('Expert (16+ edges)', '30%', Colors.purple),
                  const SizedBox(height: 8),
                  Text(
                    'Tasks with fewer than 7 edges are considered invalid.',
                    style: TextStyle(
                      fontSize: 11, 
                      color: Colors.red[600],
                      fontStyle: FontStyle.italic,
                    ),
                  ),
                ],
              ),
            ),
          ],
        );
      },
    );
  }
}