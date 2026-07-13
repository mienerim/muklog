import 'dart:convert';

import 'package:google_sign_in/google_sign_in.dart';
import 'package:http/http.dart' as http;

/// The *web* OAuth client id. On Android this is passed as `serverClientId` so
/// that Google issues an ID token whose `aud` equals this value — which is
/// exactly what the backend verifies against (`app.google.client-id`). The
/// Android OAuth client (type 1) itself is matched at runtime by package name +
/// SHA-1 in the Google Cloud project, so it is not referenced here by id.
const String kServerClientId =
    '925205002034-uvbqgamji5hiqvrkik74kmqbe3jivq2m.apps.googleusercontent.com';

/// Points at the host machine's backend through an adb reverse tunnel:
///   adb reverse tcp:8081 tcp:8081
/// Run that once per USB connection. Works for both a physical device and the
/// emulator, since the phone's own localhost:8081 is forwarded to the PC.
const String kBackendBaseUrl = 'http://localhost:8081';

class AuthService {
  AuthService._();
  static final AuthService instance = AuthService._();

  final GoogleSignIn _googleSignIn = GoogleSignIn.instance;
  bool _initialized = false;

  /// In-memory JWT storage for this session (no persistence yet).
  String? accessToken;

  Future<void> ensureInitialized() async {
    if (_initialized) return;
    await _googleSignIn.initialize(serverClientId: kServerClientId);
    _initialized = true;
  }

  Stream<GoogleSignInAuthenticationEvent> get authenticationEvents =>
      _googleSignIn.authenticationEvents;

  /// Given a signed-in Google account, exchange its ID token with our backend
  /// and store the resulting JWT. Throws on failure.
  Future<String> loginWithBackend(GoogleSignInAccount account) async {
    final idToken = account.authentication.idToken;
    if (idToken == null) {
      throw Exception('No idToken returned from Google Sign-In.');
    }

    final response = await http.post(
      Uri.parse('$kBackendBaseUrl/auth/google'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'idToken': idToken}),
    );

    if (response.statusCode != 200) {
      final body = response.body.isNotEmpty ? response.body : '(empty body)';
      throw Exception('Backend login failed (${response.statusCode}): $body');
    }

    final data = jsonDecode(response.body) as Map<String, dynamic>;
    final token = data['accessToken'] as String?;
    if (token == null) {
      throw Exception('Backend response missing accessToken.');
    }
    accessToken = token;
    return token;
  }
}
