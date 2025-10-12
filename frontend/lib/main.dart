import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'screens/left_nav_home_screen_new.dart';
import 'screens/login_screen.dart';
import 'screens/task_workflow_screen.dart';
import 'screens/task_history_screen.dart';
import 'providers/task_provider.dart';
import 'providers/auth_provider.dart';
import 'providers/task_history_provider.dart';
import 'theme/app_theme.dart';

void main() {
  runApp(const AgenticWorkstationApp());
}

class AgenticWorkstationApp extends StatelessWidget {
  const AgenticWorkstationApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AuthProvider()),
        ChangeNotifierProvider(create: (_) => TaskProvider()),
        ChangeNotifierProvider(create: (_) => TaskHistoryProvider()),
      ],
      child: MaterialApp(
        title: 'Amazon Agentic Workstation',
        theme: AppTheme.lightTheme,
        darkTheme: AppTheme.darkTheme,
        themeMode: ThemeMode.system,
        debugShowCheckedModeBanner: false,
        routes: {
          '/': (context) => const AuthenticationWrapper(),
          '/tasks': (context) => const TaskHistoryScreen(),
        },
        onGenerateRoute: _generateRoute,
      ),
    );
  }

  static Route<dynamic>? _generateRoute(RouteSettings settings) {
    // Handle dynamic routes like /task/:taskId and /task/:taskId/step/:stepIndex
    final uri = Uri.parse(settings.name ?? '');
    final pathSegments = uri.pathSegments;

    if (pathSegments.length >= 2 && pathSegments[0] == 'task') {
      final taskId = pathSegments[1];
      
      if (pathSegments.length == 2) {
        // Route: /task/:taskId
        return MaterialPageRoute(
          builder: (context) => TaskWorkflowScreen(taskId: taskId),
          settings: settings,
        );
      } else if (pathSegments.length >= 4 && pathSegments[2] == 'step') {
        // Route: /task/:taskId/step/:stepIndex
        final stepIndexStr = pathSegments[3];
        final stepIndex = int.tryParse(stepIndexStr);
        
        if (stepIndex != null) {
          return MaterialPageRoute(
            builder: (context) => TaskWorkflowScreen(
              taskId: taskId, 
              initialStepIndex: stepIndex,
            ),
            settings: settings,
          );
        }
      }
    }

    // Default fallback - redirect to home
    return MaterialPageRoute(
      builder: (context) => const AuthenticationWrapper(),
      settings: settings,
    );
  }
}

class AuthenticationWrapper extends StatefulWidget {
  const AuthenticationWrapper({Key? key}) : super(key: key);

  @override
  State<AuthenticationWrapper> createState() => _AuthenticationWrapperState();
}

class _AuthenticationWrapperState extends State<AuthenticationWrapper> {
  bool _isInitialized = false;

  @override
  void initState() {
    super.initState();
    // Add a small delay to ensure proper initialization
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        setState(() {
          _isInitialized = true;
        });
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    if (!_isInitialized) {
      return const Scaffold(
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              CircularProgressIndicator(
                valueColor: AlwaysStoppedAnimation<Color>(Colors.orange),
              ),
              SizedBox(height: 16),
              Text('Initializing...'),
            ],
          ),
        ),
      );
    }

    return Consumer<AuthProvider>(
      builder: (context, auth, child) {
        // Show loading screen while checking authentication
        if (auth.isLoading) {
          return const Scaffold(
            body: Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircularProgressIndicator(
                    valueColor: AlwaysStoppedAnimation<Color>(Colors.orange),
                  ),
                  SizedBox(height: 16),
                  Text('Loading...'),
                ],
              ),
            ),
          );
        }
        
        // Show login screen if not authenticated
        if (!auth.isAuthenticated) {
          return const LoginScreen();
        }
        
        // Show main app if authenticated
        return const AuthenticatedApp();
      },
    );
  }
}

class AuthenticatedApp extends StatelessWidget {
  const AuthenticatedApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Amazon Agentic Workstation'),
        backgroundColor: Colors.orange.shade600,
        foregroundColor: Colors.white,
        actions: [
          Consumer<AuthProvider>(
            builder: (context, auth, child) {
              return PopupMenuButton<String>(
                icon: const Icon(Icons.account_circle),
                onSelected: (value) {
                  if (value == 'logout') {
                    _showLogoutDialog(context, auth);
                  }
                },
                itemBuilder: (context) => [
                  PopupMenuItem<String>(
                    enabled: false,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Logged in as:',
                          style: TextStyle(
                            fontSize: 12,
                            color: Colors.grey.shade600,
                          ),
                        ),
                        Text(
                          auth.userId ?? 'Unknown',
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const PopupMenuDivider(),
                  const PopupMenuItem<String>(
                    value: 'logout',
                    child: Row(
                      children: [
                        Icon(Icons.logout),
                        SizedBox(width: 8),
                        Text('Logout'),
                      ],
                    ),
                  ),
                ],
              );
            },
          ),
        ],
      ),
      body: const LeftNavHomeScreenNew(),
    );
  }

  void _showLogoutDialog(BuildContext context, AuthProvider auth) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Confirm Logout'),
        content: const Text('Are you sure you want to log out?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.of(context).pop();
              auth.logout();
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.red.shade600,
              foregroundColor: Colors.white,
            ),
            child: const Text('Logout'),
          ),
        ],
      ),
    );
  }
}
