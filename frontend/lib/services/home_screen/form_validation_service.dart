import '../../providers/task_provider.dart';

/// Service for validating form state and checking dirty status
class FormValidationService {
  /// Check if a specific section has unsaved changes
  static bool isSectionDirty(TaskProvider provider, String? sectionKey) {
    if (sectionKey == null) return false;
    
    switch (sectionKey) {
      case 'repo':
        return provider.dirtyRepo;
      case 'params':
        return provider.dirtyParams;
      case 'instruction':
        return provider.dirtyInstruction;
      case 'actions':
        return provider.dirtyActions;
      case 'user':
        return provider.dirtyUserId;
      case 'outputs':
        return provider.dirtyOutputs;
      case 'edges':
        return provider.dirtyEdges;
      default:
        return false;
    }
  }

  /// Check if any section has unsaved changes
  static bool hasAnyDirty(TaskProvider provider) {
    return provider.dirtyRepo ||
           provider.dirtyParams ||
           provider.dirtyInstruction ||
           provider.dirtyActions ||
           provider.dirtyUserId ||
           provider.dirtyOutputs ||
           provider.dirtyEdges;
  }
}