import 'dart:async';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../../config/app_config.dart';
import '../providers/auth_provider.dart';

final apiServiceProvider = Provider<ApiService>((ref) {
  return ApiService(AppConfig.baseUrl, ref);
});

class ApiService {
  static const _storage = FlutterSecureStorage();
  static const _refreshTokenKey = 'refresh_token';

  static const _retriedFlag = 'retried_after_refresh';

  final Ref _ref;
  late final Dio _dio;

  Completer<Map<String, String>?>? _refreshCompleter;

  ApiService(String baseUrl, this._ref) {
    _dio = Dio(BaseOptions(
      baseUrl: baseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 15),
      headers: {'Content-Type': 'application/json'},
    ));

    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: _attachToken,
        onError: _handleError,
      ),
    );
  }

  Future<void> _attachToken(
      RequestOptions options,
      RequestInterceptorHandler handler,
      ) async {
    final token = await _storage.read(key: 'access_token');
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  Future<void> _handleError(
      DioException error,
      ErrorInterceptorHandler handler,
      ) async {
    final response = error.response;
    final request = error.requestOptions;

    final isUnauthorized = response?.statusCode == 401;
    final alreadyRetried = request.extra[_retriedFlag] == true;
    final isRefreshCall = request.path.contains('/api/auth/refresh');

    if (!isUnauthorized || alreadyRetried || isRefreshCall) {
      return handler.next(error);
    }

    final newTokens = await _refreshAccessToken();

    if (newTokens == null) {
      await _ref.read(authStateProvider.notifier).logout();
      return handler.next(error);
    }

    try {
      request.extra[_retriedFlag] = true;
      request.headers['Authorization'] = 'Bearer ${newTokens['accessToken']}';
      final retried = await _dio.fetch(request);
      return handler.resolve(retried);
    } catch (_) {
      return handler.next(error);
    }
  }

  Future<Map<String, String>?> _refreshAccessToken() async {
    if (_refreshCompleter != null) {
      return _refreshCompleter!.future;
    }

    final completer = Completer<Map<String, String>?>();
    _refreshCompleter = completer;

    try {
      final refreshToken = await _storage.read(key: _refreshTokenKey);

      if (refreshToken == null) {
        completer.complete(null);
        return null;
      }

      final plainDio = Dio(BaseOptions(baseUrl: _dio.options.baseUrl));

      final response = await plainDio.post('/api/auth/refresh', data: {
        'refreshToken': refreshToken,
      });

      final data = response.data as Map<String, dynamic>;
      final newAccessToken = data['accessToken'] as String;
      final newRefreshToken = data['refreshToken'] as String;

      await _ref.read(authStateProvider.notifier).updateTokens(
        accessToken: newAccessToken,
        refreshToken: newRefreshToken,
      );

      final result = {
        'accessToken': newAccessToken,
        'refreshToken': newRefreshToken,
      };
      completer.complete(result);
      return result;
    } catch (_) {
      completer.complete(null);
      return null;
    } finally {
      _refreshCompleter = null;
    }
  }

  Future<Response> get(
      String path, {
        Map<String, dynamic>? queryParameters,
      }) =>
      _dio.get(path, queryParameters: queryParameters);

  Future<Response> post(String path, {dynamic data}) =>
      _dio.post(path, data: data);

  Future<Response> patch(String path, {dynamic data}) =>
      _dio.patch(path, data: data);

  Future<Response> delete(String path) => _dio.delete(path);
}