import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/task_provider.dart';
import '../../services/home_screen/navigation_service.dart';
import '../../services/home_screen/form_validation_service.dart';
import '../../services/home_screen/ui_helper_service.dart';

/// Left navigation widget for the home screen
class LeftNavigationWidget extends StatelessWidget {
  final int selectedIndex;
  final Function(int) onItemSelected;

  const LeftNavigationWidget({
    super.key,
    required this.selectedIndex,
    required this.onItemSelected,
  });

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<TaskProvider>();
    
    return Container(
      width: 260,
      decoration: const BoxDecoration(
        color: Color(0xFF1f2937),
        boxShadow: [BoxShadow(color: Colors.black26, blurRadius: 6)],
      ),
      child: Column(
        children: [
          const SizedBox(height: 32),
          const Text(
            'Agentic Workstation',
            style: TextStyle(
              color: Colors.white,
              fontSize: 16,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 24),
          Expanded(
            child: ListView.builder(
              itemCount: NavigationService.itemCount,
              itemBuilder: (context, index) {
                final item = NavigationService.getItem(index);
                final selected = index == selectedIndex;
                final dirty = FormValidationService.isSectionDirty(provider, item.sectionKey);
                final enabled = _isNavigationEnabled(provider, index);
                
                return ListTile(
                  leading: Icon(
                    item.icon,
                    color: enabled 
                      ? (selected ? Colors.white : Colors.grey[400])
                      : Colors.grey[600],
                  ),
                  title: Row(
                    children: [
                      Expanded(
                        child: Text(
                          item.label,
                          style: TextStyle(
                            color: enabled
                              ? (selected ? Colors.white : Colors.grey[300])
                              : Colors.grey[600],
                            fontSize: 14,
                          ),
                        ),
                      ),
                      if (dirty && enabled)
                        const Text(
                          '•',
                          style: TextStyle(
                            color: Colors.orangeAccent,
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                    ],
                  ),
                  selected: selected,
                  selectedTileColor: const Color(0xFF374151),
                  enabled: enabled,
                  onTap: enabled ? () => _handleItemTap(context, index, provider) : null,
                );
              },
            ),
          ),
          const Padding(
            padding: EdgeInsets.all(12.0),
            child: Text(
              'v1.0.0',
              style: TextStyle(color: Colors.grey, fontSize: 12),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _handleItemTap(BuildContext context, int index, TaskProvider provider) async {
    if (index != selectedIndex && FormValidationService.hasAnyDirty(provider)) {
      final proceed = await UIHelperService.showDiscardChangesDialog(context);
      if (!proceed) return;
      
      // Discard changes by reloading from last saved state
      try {
        await provider.discardChanges();
      } catch (e) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Failed to discard changes: $e')),
          );
        }
        return; // Don't proceed with navigation if discard failed
      }
    }
    
    onItemSelected(index);
  }

  /// Check if navigation item should be enabled based on task state
  bool _isNavigationEnabled(TaskProvider provider, int index) {
    final item = NavigationService.getItem(index);
    
    // Always enabled items (Phase 1)
    switch (item.sectionKey) {
      case 'tasks_history':
      case 'repo': // Import JSON
        return true;
      
      // Task-dependent items (Phase 2)  
      case 'params': // Project Parameters
      case 'instruction':
      case 'actions': 
      case 'user':
      case 'outputs':
      case 'edges':
      case 'num_edges':
      case 'results':
        return provider.canNavigateToWorkflow;
      
      // Items without section keys (task.json, validation)
      default:
        return provider.canNavigateToWorkflow;
    }
  }
}