import 'package:flutter/material.dart';
import 'package:google_sign_in/google_sign_in.dart';

import '../services/auth_service.dart';
import 'home_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  bool _loading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    AuthService.instance.ensureInitialized().then((_) {
      AuthService.instance.authenticationEvents.listen(
        _handleAuthEvent,
        onError: (Object e) => setState(() => _error = e.toString()),
      );
    });
  }

  Future<void> _startSignIn() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      // v7 interactive sign-in. The result is delivered through the
      // authenticationEvents stream, which _handleAuthEvent listens to.
      await AuthService.instance.ensureInitialized();
      await GoogleSignIn.instance.authenticate();
    } catch (e) {
      setState(() {
        _error = e.toString();
        _loading = false;
      });
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('로그인 실패: $e')));
      }
    }
  }

  Future<void> _handleAuthEvent(GoogleSignInAuthenticationEvent event) async {
    if (event is! GoogleSignInAuthenticationEventSignIn) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await AuthService.instance.loginWithBackend(event.user);
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => const HomeScreen()),
      );
    } catch (e) {
      setState(() => _error = e.toString());
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('로그인 실패: $e')));
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('muklog 로그인')),
      body: Center(
        child: _loading
            ? const CircularProgressIndicator()
            : Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  ElevatedButton.icon(
                    onPressed: _startSignIn,
                    icon: const Icon(Icons.login),
                    label: const Text('Google로 로그인'),
                  ),
                  if (_error != null) ...[
                    const SizedBox(height: 16),
                    Text(_error!, style: const TextStyle(color: Colors.red)),
                  ],
                ],
              ),
      ),
    );
  }
}
