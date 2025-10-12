import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/task_provider.dart';
import '../widgets/home_screen/left_navigation_widget.dart';
import '../services/home_screen/content_router_service.dart';

/// Left navigation based home screen implementing new layout.
class LeftNavHomeScreenNew extends StatefulWidget {
  const LeftNavHomeScreenNew({super.key});

  @override
  State<LeftNavHomeScreenNew> createState() => _LeftNavHomeScreenNewState();
}

class _LeftNavHomeScreenNewState extends State<LeftNavHomeScreenNew> with TickerProviderStateMixin {
  int _selectedIndex = 0;
  final ScrollController _rightScroll = ScrollController();
  bool _isInitialized = false;

  @override
  void initState() {
    super.initState();
    // Ensure proper initialization order
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        setState(() {
          _isInitialized = true;
        });
      }
    });
  }

  @override
  void dispose() {
    _rightScroll.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!_isInitialized) {
      return const Scaffold(
        body: Center(
          child: CircularProgressIndicator(),
        ),
      );
    }

    return Scaffold(
      body: Consumer<TaskProvider>(
        builder: (context, provider, child) {
          // Show loading while TaskProvider is initializing
          if (provider.isInitializing) {
            return const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircularProgressIndicator(),
                  SizedBox(height: 16),
                  Text('Loading application...'),
                ],
              ),
            );
          }

          return LayoutBuilder(
            builder: (context, constraints) {
              // Ensure we have valid constraints before building
              if (constraints.maxWidth <= 0 || constraints.maxHeight <= 0) {
                return const Center(child: CircularProgressIndicator());
              }

              return Row(
                children: [
                  // Left Navigation using our extracted widget
                  LeftNavigationWidget(
                    selectedIndex: _selectedIndex,
                    onItemSelected: (index) {
                      if (mounted) {
                        setState(() {
                          _selectedIndex = index;
                        });
                      }
                    },
                  ),
                  
                  // Main Content using our content router service  
                  Expanded(
                    child: AnimatedSwitcher(
                      duration: const Duration(milliseconds: 250),
                      child: ContentRouterService.buildRightContent(
                        context,
                        provider,
                        _selectedIndex,
                        key: ValueKey(_selectedIndex),
                        scrollController: _rightScroll,
                        onNavigate: (index) {
                          if (mounted) {
                            setState(() {
                              _selectedIndex = index;
                            });
                          }
                        },
                      ),
                    ),
                  ),
                ],
              );
            },
          );
        },
      ),
    );
  }
}