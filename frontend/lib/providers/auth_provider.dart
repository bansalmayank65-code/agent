import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../services/api_service.dart';

class AuthProvider with ChangeNotifier {
  bool _isAuthenticated = false;
  String? _userId;
  String? _sessionId;
  bool _isLoading = false;
  String? _errorMessage;

  // Getters
  bool get isAuthenticated => _isAuthenticated;
  String? get userId => _userId;
  String? get sessionId => _sessionId;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;

  final ApiService _apiService = ApiService();

  AuthProvider() {
    _initializeAuth();
  }

  /// Initialize authentication state
  Future<void> _initializeAuth() async {
    _isLoading = true;
    notifyListeners();
    
    await _loadStoredSession();
    
    _isLoading = false;
    notifyListeners();
  }

  /// Load stored session from SharedPreferences
  Future<void> _loadStoredSession() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      _sessionId = prefs.getString('sessionId');
      _userId = prefs.getString('userId');
      
      if (_sessionId != null) {
        // Validate stored session with server
        await validateSession();
      }
    } catch (e) {
      if (kDebugMode) {
        print('Error loading stored session: $e');
      }
    }
  }

  /// Login with username and password
  Future<bool> login(String userId, String password, {bool rememberMe = false}) async {
    _setLoading(true);
    _clearError();

    try {
      final response = await _apiService.login(userId, password);
      
      if (response['success'] == true) {
        _sessionId = response['sessionId'];
        _userId = response['userId'];
        _isAuthenticated = true;
        
        // Store session locally
        await _storeSession();
        
        // Store credentials if remember me is checked
        if (rememberMe) {
          await _storeCredentials(userId, password);
        } else {
          await _clearCredentials();
        }
        
        _setLoading(false);
        return true;
      } else {
        _setError(response['message'] ?? 'Login failed');
        _setLoading(false);
        return false;
      }
    } catch (e) {
      _setError('Network error. Please try again.');
      _setLoading(false);
      return false;
    }
  }

  /// Logout user
  Future<void> logout() async {
    try {
      if (_sessionId != null) {
        await _apiService.logout(_sessionId!);
      }
    } catch (e) {
      if (kDebugMode) {
        print('Error during logout: $e');
      }
    } finally {
      await _clearSession();
      // Keep credentials if remember me is enabled
      final rememberMe = await isRememberMeEnabled();
      if (!rememberMe) {
        await _clearCredentials();
      }
      _isAuthenticated = false;
      _userId = null;
      _sessionId = null;
      notifyListeners();
    }
  }

  /// Validate current session
  Future<bool> validateSession() async {
    if (_sessionId == null) return false;

    try {
      final isValid = await _apiService.validateSession(_sessionId!);
      if (!isValid) {
        await _clearSession();
        _isAuthenticated = false;
        _userId = null;
        _sessionId = null;
        notifyListeners();
      } else {
        _isAuthenticated = true;
        notifyListeners();
      }
      return isValid;
    } catch (e) {
      if (kDebugMode) {
        print('Session validation error: $e');
      }
      return false;
    }
  }

  /// Store session to SharedPreferences
  Future<void> _storeSession() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      if (_sessionId != null) {
        await prefs.setString('sessionId', _sessionId!);
      }
      if (_userId != null) {
        await prefs.setString('userId', _userId!);
      }
    } catch (e) {
      if (kDebugMode) {
        print('Error storing session: $e');
      }
    }
  }

  /// Clear stored session
  Future<void> _clearSession() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove('sessionId');
      await prefs.remove('userId');
    } catch (e) {
      if (kDebugMode) {
        print('Error clearing session: $e');
      }
    }
  }

  /// Store credentials for "Remember Me" feature
  Future<void> _storeCredentials(String userId, String password) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('rememberedUserId', userId);
      await prefs.setString('rememberedPassword', password);
      await prefs.setBool('rememberMe', true);
    } catch (e) {
      if (kDebugMode) {
        print('Error storing credentials: $e');
      }
    }
  }

  /// Clear stored credentials
  Future<void> _clearCredentials() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove('rememberedUserId');
      await prefs.remove('rememberedPassword');
      await prefs.remove('rememberMe');
    } catch (e) {
      if (kDebugMode) {
        print('Error clearing credentials: $e');
      }
    }
  }

  /// Get stored credentials
  Future<Map<String, String?>> getStoredCredentials() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final rememberMe = prefs.getBool('rememberMe') ?? false;
      if (rememberMe) {
        return {
          'userId': prefs.getString('rememberedUserId'),
          'password': prefs.getString('rememberedPassword'),
        };
      }
    } catch (e) {
      if (kDebugMode) {
        print('Error getting stored credentials: $e');
      }
    }
    return {'userId': null, 'password': null};
  }

  /// Check if remember me is enabled
  Future<bool> isRememberMeEnabled() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      return prefs.getBool('rememberMe') ?? false;
    } catch (e) {
      if (kDebugMode) {
        print('Error checking remember me: $e');
      }
      return false;
    }
  }

  /// Set loading state
  void _setLoading(bool loading) {
    _isLoading = loading;
    notifyListeners();
  }

  /// Set error message
  void _setError(String error) {
    _errorMessage = error;
    notifyListeners();
  }

  /// Clear error message
  void _clearError() {
    _errorMessage = null;
    notifyListeners();
  }

  /// Check if user needs to login
  bool get needsLogin => !_isAuthenticated;

  /// Get authorization header for API calls
  String? get authorizationHeader {
    if (_sessionId != null) {
      return 'Bearer $_sessionId';
    }
    return null;
  }
}