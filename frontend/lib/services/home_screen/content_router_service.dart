import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../providers/task_provider.dart';
// TODO: TEMPORARILY COMMENTED - import '../../screens/task_history_screen.dart';
import '../../widgets/home_screen/repository_section_widget.dart';
import '../../widgets/home_screen/project_parameters_section_widget.dart';

import '../../widgets/home_screen/instruction_section_widget.dart';
import '../../widgets/home_screen/actions_section_widget.dart';
import '../../widgets/home_screen/user_id_section_widget.dart';
import '../../widgets/home_screen/outputs_section_widget.dart';
import '../../widgets/home_screen/edges_section_widget.dart';
import '../../widgets/home_screen/number_of_edges_section_widget.dart';
import '../../widgets/home_screen/task_json_section_widget.dart';
import '../../widgets/home_screen/validation_section_widget.dart';
import '../../widgets/home_screen/results_section_widget.dart';
import '../../services/home_screen/ui_helper_service.dart';
import '../../screens/task_interface_converter_screen.dart';
import '../../screens/edge_merger_screen.dart';
import '../../screens/task_refiner_screen.dart';
import '../../widgets/common/developer_footer.dart';

/// Service for routing content sections
class ContentRouterService {
  /// Build the right content based on selected index
  static Widget buildRightContent(
    BuildContext context,
    TaskProvider provider,
    int selectedIndex, {
    Key? key,
    required ScrollController scrollController,
    Function(int)? onNavigate,
  }) {
    return FocusableActionDetector(
      shortcuts: {
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.keyS): const ActivateIntent(),
      },
      actions: {
        ActivateIntent: CallbackAction<ActivateIntent>(
          onInvoke: (_) {
            UIHelperService.saveCurrentSection(context, provider, advance: false);
            return null;
          },
        ),
      },
      child: _buildSectionContent(context, provider, selectedIndex, scrollController, onNavigate: onNavigate, key: key),
    );
  }

  /// Build section content based on index
  static Widget _buildSectionContent(
    BuildContext context,
    TaskProvider provider,
    int selectedIndex,
    ScrollController scrollController, {
    Key? key,
    Function(int)? onNavigate,
  }) {
    switch (selectedIndex) {
      // case 0: Tasks History is temporarily hidden
      case 0:
        return _sectionWrapper(
          key,
          'Select local repository folder',
          RepositorySectionWidget(provider: provider),
          scrollController,
        );
      case 1:
        return _sectionWrapper(
          key,
          'Edit project parameters',
          ProjectParametersSectionWidget(provider: provider),
          scrollController,
        );
      case 2:
        return _sectionWrapper(
          key,
          'Instruction',
          const InstructionSectionWidget(),
          scrollController,
        );
      case 3:
        return _sectionWrapper(
          key,
          'Actions',
          const ActionsSectionWidget(),
          scrollController,
        );
      case 4:
        return _sectionWrapper(
          key,
          'User ID',
          const UserIdSectionWidget(),
          scrollController,
        );
      case 5:
        return _sectionWrapper(
          key,
          'Outputs',
          const OutputsSectionWidget(),
          scrollController,
        );
      case 6:
        return _sectionWrapper(
          key,
          'Edges',
          const EdgesSectionWidget(),
          scrollController,
        );
      case 7:
        return _sectionWrapper(
          key,
          'Number of edges',
          const NumberOfEdgesSectionWidget(),
          scrollController,
        );
      case 8:
        return _sectionWrapper(
          key,
          'Task.json',
          const TaskJsonSectionWidget(),
          scrollController,
        );
      case 9:
        return _sectionWrapper(
          key,
          'Validate Task.json',
          const ValidationSectionWidget(),
          scrollController,
        );
      case 10:
        return _sectionWrapper(
          key,
          'Result.json',
          ResultsSectionWidget(onNavigate: onNavigate),
          scrollController,
        );
      case 11:
        // Separator - return empty (shouldn't be selected)
        return const SizedBox();
      case 12:
        return _sectionWrapper(
          key,
          'HR Expert Interface Changer',
          const TaskInterfaceConverterScreen(standalone: false),
          scrollController,
        );
      case 13:
        return _sectionWrapper(
          key,
          'Merge Edges',
          const EdgeMergerScreen(standalone: false),
          scrollController,
        );
      case 14:
        return _sectionWrapper(
          key,
          'Refine task.json',
          const TaskRefinerScreen(standalone: false),
          scrollController,
        );
      default:
        return const SizedBox();
    }
  }

  /// Common section wrapper
  static Widget _sectionWrapper(
    Key? key,
    String title,
    Widget child,
    ScrollController scrollController,
  ) {
    return Container(
      key: key,
      color: const Color(0xFFF9FAFB),
      child: LayoutBuilder(
        builder: (context, constraints) {
          // Ensure we have valid layout constraints
          if (constraints.maxWidth <= 0 || constraints.maxHeight <= 0) {
            return const Center(child: CircularProgressIndicator());
          }

          return Scrollbar(
            controller: scrollController,
            child: SingleChildScrollView(
              controller: scrollController,
              padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF111827),
                    ),
                  ),
                  const SizedBox(height: 16),
                  // Wrap child in error boundary
                  Builder(
                    builder: (context) {
                      try {
                        return child;
                      } catch (e) {
                        return Container(
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: Colors.red.shade50,
                            border: Border.all(color: Colors.red.shade200),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Column(
                            children: [
                              Icon(Icons.error_outline, color: Colors.red.shade600),
                              const SizedBox(height: 8),
                              Text(
                                'Error loading section: $title',
                                style: TextStyle(color: Colors.red.shade800),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                e.toString(),
                                style: TextStyle(
                                  color: Colors.red.shade600,
                                  fontSize: 12,
                                ),
                              ),
                            ],
                          ),
                        );
                      }
                    },
                  ),
                  // Add developer footer at the bottom
                  const SizedBox(height: 48),
                  const DeveloperFooter(),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}